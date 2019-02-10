package com.ivanovsky.passnotes.presentation.core;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
