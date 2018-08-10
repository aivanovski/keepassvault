package com.ivanovsky.passnotes.presentation.core;

import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public abstract class BaseActivity extends AppCompatActivity {

	public void initCurrentActionBar(Toolbar toolbar) {
		setSupportActionBar(toolbar);
	}

	@SuppressWarnings("ConstantConditions")
	@NonNull
	public ActionBar getCurrentActionBar() {
		return getSupportActionBar();
	}
}
