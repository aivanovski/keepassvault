package com.ivanovsky.passnotes.presentation.unlock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.presentation.core.BaseActivity;

public class UnlockActivity extends BaseActivity {

	public static Intent createStartIntent(Context context) {
		return new Intent(context, UnlockActivity.class);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.core_base_activity);

		setSupportActionBar(findViewById(R.id.tool_bar));
		getCurrentActionBar().setTitle(R.string.app_name);

		UnlockFragment fragment = UnlockFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		UnlockPresenter presenter = new UnlockPresenter(this, fragment);
		fragment.setPresenter(presenter);
	}
}
