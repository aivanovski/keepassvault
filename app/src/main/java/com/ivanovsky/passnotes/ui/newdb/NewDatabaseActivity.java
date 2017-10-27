package com.ivanovsky.passnotes.ui.newdb;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.db.AppDatabase;
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding;
import com.ivanovsky.passnotes.ui.core.BaseActivity;

import javax.inject.Inject;

public class NewDatabaseActivity extends BaseActivity {

	@Inject
	AppDatabase db;

	private NewDatabasePresenter presenter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getDaggerComponent().inject(this);

		CoreBaseActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.core_base_activity);

		setSupportActionBar(binding.toolBar);
		getCurrentActionBar().setTitle(R.string.app_name);
		getCurrentActionBar().setDisplayHomeAsUpEnabled(true);

		NewDatabaseFragment fragment = NewDatabaseFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		presenter = new NewDatabasePresenter(fragment, this);
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
