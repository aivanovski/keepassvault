package com.ivanovsky.passnotes.presentation.newdb;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.presentation.core.BaseActivity;

import javax.inject.Inject;

public class NewDatabaseActivity extends BaseActivity {

	@Inject
	AppDatabase db;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Injector.getInstance().getAppComponent().inject(this);

		setContentView(R.layout.core_base_activity);

		setSupportActionBar(findViewById(R.id.tool_bar));
		getCurrentActionBar().setTitle(R.string.app_name);
		getCurrentActionBar().setDisplayHomeAsUpEnabled(true);

		NewDatabaseFragment fragment = NewDatabaseFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		NewDatabasePresenter presenter = new NewDatabasePresenter(fragment, this);
		fragment.setPresenter(presenter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
