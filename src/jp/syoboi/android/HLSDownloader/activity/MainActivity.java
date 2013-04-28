package jp.syoboi.android.HLSDownloader.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import jp.syoboi.android.HLSDownloader.service.DownloadService;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);


		Intent i = getIntent();
		if (Intent.ACTION_VIEW.equals(i.getAction())) {
			startDownload();
			finish();
		} else {
			finish();
		}
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}

	void startDownload() {
		Intent i = new Intent(this, DownloadService.class);
		i.setAction(Intent.ACTION_INSERT);
		i.setData(getIntent().getData());
		i.putExtras(getIntent().getExtras());
		startService(i);
	}
}
