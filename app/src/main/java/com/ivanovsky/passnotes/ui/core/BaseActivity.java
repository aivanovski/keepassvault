package com.ivanovsky.passnotes.ui.core;

import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

	@SuppressWarnings("ConstantConditions")
	@NonNull
	protected ActionBar getCurrentActionBar() {
		return getSupportActionBar();
	}
}
