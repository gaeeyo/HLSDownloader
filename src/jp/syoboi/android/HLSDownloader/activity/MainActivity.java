package jp.syoboi.android.HLSDownloader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import jp.syoboi.android.HLSDownloader.R;
import jp.syoboi.android.HLSDownloader.service.DownloadService;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		Intent i = getIntent();
		if (Intent.ACTION_VIEW.equals(i.getAction())) {
			setContentView(R.layout.main_activity);
			startDownload();
//			finish();
		} else {
			finish();
		}
	}

	void startDownload() {
		Intent i = new Intent(this, DownloadService.class);
		i.setAction(Intent.ACTION_INSERT);
		i.setData(getIntent().getData());
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			i.putExtras(extras);
		}
		startService(i);
	}
}
