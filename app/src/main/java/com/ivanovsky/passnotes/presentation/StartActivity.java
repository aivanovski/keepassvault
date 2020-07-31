package com.ivanovsky.passnotes.presentation;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.injection.DaggerInjector;
import com.ivanovsky.passnotes.presentation.core.BaseActivity;
import com.ivanovsky.passnotes.presentation.unlock.UnlockActivity;

public class StartActivity extends BaseActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DaggerInjector.getInstance().getAppComponent().inject(this);

		startActivity(new Intent(this, UnlockActivity.class));
		finish();
	}
}
