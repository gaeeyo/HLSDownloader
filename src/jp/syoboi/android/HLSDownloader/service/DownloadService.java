package jp.syoboi.android.HLSDownloader.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import jp.syoboi.android.HLSDownloader.App;

public class DownloadService extends Service {

	static final String TAG = "DownloadService";

	static int 						sTaskId = 0;
	HashMap<String,DownloadTask>	mTaskMap = new HashMap<String,DownloadTask>();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (App.DEBUG) {
			Log.d(TAG, "onCreate");
		}
	}

	@Override
	public void onDestroy() {
		if (App.DEBUG) {
			Log.d(TAG, "onDestroy");
		}
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			final String m3u8 = intent.getDataString();

			if (Intent.ACTION_INSERT.equals(action)) {

				String title = intent.getStringExtra(Intent.EXTRA_TITLE);
				if (TextUtils.isEmpty(title)) {
					title = m3u8;
				}

				DownloadTask task = mTaskMap.get(m3u8);


				if (task == null) {
					task = new DownloadTask(this, sTaskId++, m3u8, title) {
						@Override
						protected void onPostExecute(Object result) {
							super.onPostExecute(result);
							mTaskMap.remove(m3u8);
							stopOrContinue();
						}
						@Override
						protected void onCancelled(Object result) {
							super.onCancelled(result);
							mTaskMap.remove(m3u8);
							stopOrContinue();
						}
					};
					mTaskMap.put(m3u8, task);
					task.execute();
				}
			}
			else if (Intent.ACTION_DELETE.equals(action)) {
				DownloadTask task = mTaskMap.get(m3u8);
				if (task != null) {
					task.cancel(true);
				}
			}
		}
		stopOrContinue();
		return START_STICKY;
	}

	void stopOrContinue() {
		if (mTaskMap.size() == 0) {
			stopSelf();
		}
	}
}
