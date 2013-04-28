package jp.syoboi.android.HLSDownloader;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import jp.syoboi.android.HLSDownloader.utils.ModernAsyncTask;

public class App extends Application {
	public static boolean DEBUG = false;

	// Activity
	public static final String ACTION_DOWNLOAD_NOTIFICATION_CLICKED = "action.download.notification.clicked";

	// Notification
	public static final int NOTIFY_ID_DOWNLOADING = 1000;
	public static final int NOTIFY_ID_DOWNLOAD_RESULT = 2000;

	@Override
	public void onCreate() {
		super.onCreate();

		DEBUG = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

		ModernAsyncTask dummy = new ModernAsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				return null;
			}
		};
	}
}
