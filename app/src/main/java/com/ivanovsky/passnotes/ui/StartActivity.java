package com.ivanovsky.passnotes.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.db.AppDatabase;
import com.ivanovsky.passnotes.db.model.UsedFile;
import com.ivanovsky.passnotes.ui.core.BaseActivity;

import java.util.List;

import javax.inject.Inject;

public class StartActivity extends BaseActivity {

	private static final String TAG = StartActivity.class.getSimpleName();

	@Inject
	AppDatabase database;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getDaggerComponent().inject(this);

		new Thread(() -> {
			List<UsedFile> usedFiles = database.getUsedFileDao().getAll();
			Log.d(TAG, "usedFiles=" + usedFiles);
		}).start();

//		new Thread(() -> {
//
//			try {
//				Thread.sleep(800);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//			new Handler(Looper.getMainLooper())
//					.post(() -> {
//						startActivity(new Intent(this, OpenFileActivity.class));
//						finish();
//					});
//
//		}).start();
	}
}
