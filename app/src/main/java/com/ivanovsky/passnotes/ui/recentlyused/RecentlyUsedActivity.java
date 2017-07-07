package com.ivanovsky.passnotes.ui.recentlyused;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding;
import com.ivanovsky.passnotes.ui.core.BaseActivity;
import com.ivanovsky.passnotes.ui.core.FragmentState;

public class RecentlyUsedActivity extends BaseActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CoreBaseActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.core_base_activity);

		setSupportActionBar(binding.toolBar);
		getCurrentActionBar().setTitle(R.string.app_name);

		RecentlyUsedFragment fragment = RecentlyUsedFragment.newInstance();

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragment_container, fragment)
				.commit();
	}
}
