package com.ivanovsky.passnotes.presentation.newgroup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.presentation.core.BaseActivity;

public class NewGroupActivity extends BaseActivity {

	public static Intent createStartIntent(Context context) {
		return new Intent(context, NewGroupActivity.class);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.core_base_activity);

		setSupportActionBar(findViewById(R.id.tool_bar));
		getCurrentActionBar().setTitle(R.string.new_group);
		getCurrentActionBar().setDisplayHomeAsUpEnabled(true);

		NewGroupFragment fragment = NewGroupFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		NewGroupContract.Presenter presenter = new NewGroupPresenter(this, fragment);
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