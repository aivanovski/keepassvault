package com.ivanovsky.passnotes.ui.notepads;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding;
import com.ivanovsky.passnotes.ui.core.BaseActivity;

public class NotepadsActivity extends BaseActivity {

	private static final String EXTRA_DB_NAME = "dbName";

	public static Intent createIntent(Context context, @NonNull String dbName) {
		Intent intent = new Intent(context, NotepadsActivity.class);
		intent.putExtra(EXTRA_DB_NAME, dbName);
		return intent;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CoreBaseActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.core_base_activity);

		setSupportActionBar(binding.toolBar);
		getCurrentActionBar().setTitle(R.string.notepads);

		NotepadsFragment fragment = NotepadsFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		String dbName = null;

		if (getIntent() != null && getIntent().getExtras() != null) {
			Bundle extras = getIntent().getExtras();

			dbName = extras.getString(EXTRA_DB_NAME);
		}

		NotepadsContract.Presenter presenter = new NotepadsPresenter(this, fragment, dbName);
		fragment.setPresenter(presenter);
	}
}
