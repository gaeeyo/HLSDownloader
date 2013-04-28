package jp.syoboi.android.HLSDownloader.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import jp.syoboi.android.HLSDownloader.App;
import jp.syoboi.android.HLSDownloader.R;
import jp.syoboi.android.HLSDownloader.service.DownloadService;

public class DialogActivity extends Activity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showDialog(1);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {

		Intent i = getIntent();
		String action = i.getAction();

		if (App.ACTION_DOWNLOAD_NOTIFICATION_CLICKED.equals(action)) {
			return onDownloadNotificationClicked();
		}
		else {
			finish();
		}

		return super.onCreateDialog(id);
	}

	Dialog onDownloadNotificationClicked() {

		Dialog dlg = new AlertDialog.Builder(this)
			.setMessage(R.string.confirmCancelDownload)
			.setPositiveButton(android.R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent i = new Intent(DialogActivity.this, DownloadService.class);
					i.setAction(Intent.ACTION_DELETE);
					i.setData(Uri.parse(getIntent().getDataString()));
					startService(i);
					finish();
				}
			})
			.setNeutralButton(android.R.string.no, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.create();

		dlg.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
		return dlg;
	}
}
