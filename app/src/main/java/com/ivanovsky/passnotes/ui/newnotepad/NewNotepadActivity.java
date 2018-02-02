package com.ivanovsky.passnotes.ui.newnotepad;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding;
import com.ivanovsky.passnotes.ui.core.BaseActivity;

public class NewNotepadActivity extends BaseActivity {

	public static Intent createIntent(Context context) {
		return new Intent(context, NewNotepadActivity.class);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CoreBaseActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.core_base_activity);

		setSupportActionBar(binding.toolBar);
		getCurrentActionBar().setTitle(R.string.new_notepad);

		NewNotepadFragment fragment = NewNotepadFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();

		NewNotepadContract.Presenter presenter = new NewNotepadPresenter(this, fragment);
		fragment.setPresenter(presenter);
	}
}
