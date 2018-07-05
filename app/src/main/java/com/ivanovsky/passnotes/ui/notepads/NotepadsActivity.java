package com.ivanovsky.passnotes.ui.notepads;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.DbDescriptor;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider;
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding;
import com.ivanovsky.passnotes.ui.core.BaseActivity;

import javax.inject.Inject;

public class NotepadsActivity extends BaseActivity {

	@Inject
	EncryptedDatabaseProvider dbProvider;

	public static Intent createIntent(Context context) {
		return new Intent(context, NotepadsActivity.class);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getDaggerComponent().inject(this);

		CoreBaseActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.core_base_activity);

		setSupportActionBar(binding.toolBar);
		getCurrentActionBar().setTitle(R.string.notepads);
		getCurrentActionBar().setDisplayHomeAsUpEnabled(true);

		NotepadsFragment fragment = NotepadsFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		NotepadsContract.Presenter presenter = new NotepadsPresenter(this, fragment);
		fragment.setPresenter(presenter);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {

			if (dbProvider.isOpened()) {
				dbProvider.close();
			}

			onBackPressed();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
