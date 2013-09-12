package jp.syoboi.android.HLSDownloader.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.syoboi.android.HLSDownloader.App;
import jp.syoboi.android.HLSDownloader.R;
import jp.syoboi.android.HLSDownloader.activity.DialogActivity;
import jp.syoboi.android.HLSDownloader.utils.ModernAsyncTask;

/**
	 * 動画のダウンロードタスク
	 */
class DownloadTask extends ModernAsyncTask<Object,Object,Object> {

	private static final String TAG = "DownloadTask";

	private NotificationManager mNm;
	private Notification	mNotification;
	private Context			mContext;
	private PendingIntent	mPi;
	private String			mM3u8;

	private int				mUrlCount;
	private int				mDownloadedCount;
	private long			mDownloadedSize = 0;

	private File			mDstFile;
	private long			mDownloadProgressUpdated;
	private int				mId;
	private Exception		mThreadException;
	private Queue<byte[]>	mBuffers = new ConcurrentLinkedQueue<byte[]>();
	private FileOutputStream mOutput;

	private volatile int	mWriteIdx = 0;


	public DownloadTask(Context context, int id, String m3u8, String title) {
		mId = id;
		mM3u8 = m3u8;
		mContext = context;
		mNm = (NotificationManager) context.getSystemService(DownloadService.NOTIFICATION_SERVICE);
		mDstFile = getDstFile(title);

		// ダウンロード通知がタップされたときの PendingIntent を作成
		Intent intent = new Intent(mContext, DialogActivity.class);
		intent.setData(Uri.parse(m3u8));
		intent.setAction(App.ACTION_DOWNLOAD_NOTIFICATION_CLICKED);
		mPi = PendingIntent.getActivity(mContext, id, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

	}

	@Override
	protected void onPostExecute(Object result) {
		if (App.DEBUG) {
			Log.d(DownloadService.TAG, "bufferes size:" + mBuffers.size());
		}
		mBuffers.clear();
		super.onPostExecute(result);
	}

	@Override
	protected Object doInBackground(Object... params) {

		showDownloadProgress();
		boolean success = false;
		long start = System.currentTimeMillis();

		ExecutorService svc = Executors.newFixedThreadPool(6);
		try {

			mDstFile.delete();
			mOutput = new FileOutputStream(mDstFile);

			List<String> urls = downloadM3u(mM3u8);
			mUrlCount = urls.size();

			try {
				int idx = 0;
				for (final String url: urls) {

					final int fileIdx = idx++;
					svc.execute(new Runnable() {

						@Override
						public void run() {
							String tsUrl;
							try {
								tsUrl = new URI(mM3u8).resolve(url).toString();
								downloadFile(fileIdx, tsUrl);
							} catch (Exception e) {
								e.printStackTrace();
								mThreadException = e;
								cancel(true);
							}
						}
					});
				}
			} finally {
				svc.shutdown();
			}
			while (true) {
				if (svc.awaitTermination(500, TimeUnit.MILLISECONDS)) {
					break;
				}
			}

			long end = System.currentTimeMillis();

			Log.d(DownloadService.TAG, "ダウンロード時間:" + (end - start) / DateUtils.SECOND_IN_MILLIS + "sec" );

			if (!isCancelled()) {
				success = true;
				showSuccessNotification();
			}
			return null;
		} catch (Exception e) {
			if (mThreadException != null) {
				e = mThreadException;
			}
			mDstFile.delete();
			svc.shutdownNow();
			e.printStackTrace();
			if (!isCancelled()) {
				showErrorNotification(e);
			}
			return e;
		} finally {
			mNm.cancel(App.NOTIFY_ID_DOWNLOADING + mId);
			if (mOutput != null) {
				try {
					mOutput.close();
					mOutput = null;
					if (!success) {
						mDstFile.delete();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * エラー通知
	 * @param e
	 */
	@SuppressWarnings("deprecation")
	void showErrorNotification(Exception e) {
		Notification notification = new Notification(
				android.R.drawable.stat_notify_error,
				mContext.getString(R.string.downloadError),
				System.currentTimeMillis());

		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(mContext,
				mContext.getString(R.string.downloadError),
				String.valueOf(e.getMessage()),
				mPi);
		mNm.notify(App.NOTIFY_ID_DOWNLOAD_RESULT + mId, notification);
	}

	/**
	 * ダウンロード完了通知
	 */
	@SuppressWarnings("deprecation")
	void showSuccessNotification() {
		Notification notification = new Notification(
				android.R.drawable.stat_sys_download_done,
				mContext.getString(R.string.downloadComplete),
				System.currentTimeMillis());

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.fromFile(mDstFile), "video/ts");

		PendingIntent pi = PendingIntent.getActivity(mContext, mId, i,
				PendingIntent.FLAG_ONE_SHOT);

		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(mContext,
				mContext.getString(R.string.downloadComplete),
				mDstFile.getName(),
				pi);
		mNm.notify(App.NOTIFY_ID_DOWNLOAD_RESULT + mId, notification);
	}

	/**
	 * 進捗通知
	 */
	@SuppressWarnings("deprecation")
	void showDownloadProgress() {
		final long MB = 1024*1024;
		long now = System.currentTimeMillis();
		if (now - mDownloadProgressUpdated > DateUtils.SECOND_IN_MILLIS) {
			mDownloadProgressUpdated = now;

			String msg;
			if (mUrlCount > 0) {
				msg = String.format(Locale.ENGLISH,
					"%dMB (%d%%) %s",
					mDownloadedSize / MB,
					mDownloadedCount * 100 / mUrlCount,
					mDstFile.getName());
			} else {
				msg = mDstFile.getName();
			}

			if (mNotification == null) {
				mNotification = new Notification(
						android.R.drawable.stat_sys_download,
						mContext.getString(R.string.downloading),
						System.currentTimeMillis());
				mNotification.flags = Notification.FLAG_ONGOING_EVENT;
			}
			mNotification.setLatestEventInfo(mContext,
					mContext.getString(R.string.downloading),
					msg, mPi);

			mNm.notify(App.NOTIFY_ID_DOWNLOADING + mId, mNotification);
		}
	}

	private int downloadFile(int index, String url) throws MalformedURLException, IOException, InterruptedException {
		InputStream is = null;
		int totalReadSize = 0;
		try {
			if (App.DEBUG) {
				Log.d(DownloadService.TAG, "downloadFile url:" + url);
			}
			is = new URL(url).openStream();

			byte [] buf = mBuffers.poll();
			if (buf == null) {
				buf = new byte [300 * 1024];
			}

			int pos = 0;
			int readSize;

			while ((readSize = is.read(buf, pos, buf.length - pos)) >= 0) {
				pos += readSize;

				int remain = buf.length - pos;
				if (remain == 0) {
					// メモリが足りなくなったら増やす
					int newBufSize = buf.length * 3 / 2;
					Log.d(TAG, "Grow buf " + buf.length + " => " + newBufSize);
					byte [] newBuf = new byte [newBufSize];
					System.arraycopy(buf, 0, newBuf, 0, pos);
					buf = newBuf;
					break;
				}
			}

			if (App.DEBUG) {
				if (pos > buf.length) {
					throw new IOException("取得したデータの長さが異常でした");
//						Log.w(TAG, "pos:" + pos + " length:" + length);
				}
			}

			mDownloadedCount++;

			write(index, buf, pos);

			showDownloadProgress();

		} finally {
			if (is != null) {
				is.close();
			}
		}
		return totalReadSize;
	}

	void write(int index, byte [] buf, int length) throws InterruptedException, IOException {
		mDownloadedSize += length;

		while (true) {
			if (mWriteIdx == index) {
				synchronized (mOutput) {
					mOutput.write(buf, 0, length);
					mWriteIdx++;
				}
				mBuffers.add(buf);
				break;
			}
			Thread.sleep(125);
		}
	}

	/**
	 * m3uファイルをダウンロードして、記述されているURLの配列を返す
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	static ArrayList<String> downloadM3u(String url) throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new URL(url).openStream(), "utf-8"));
		String line;

		ArrayList<String> urls = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			if (line.startsWith("#")) {
				continue;
			}
			if (line.length() > 0) {
				urls.add(line);
			}
		}
		return urls;
	}

	static File getDstFile(String title) {
		String fileName = convertFilename(title);

		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
		dir.mkdirs();
		File file = new File(dir, fileName + ".mts");
		return file;
	}

	static String convertFilename(String title) {
		return title.replaceAll("./:\\*\\?\\|<>", "_");
	}
}