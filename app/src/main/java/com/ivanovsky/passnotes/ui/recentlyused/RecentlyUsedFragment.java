package com.ivanovsky.passnotes.ui.recentlyused;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.RecentlyUsedFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;

public class RecentlyUsedFragment extends BaseFragment {

	public static RecentlyUsedFragment newInstance() {
		return new RecentlyUsedFragment();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		RecentlyUsedFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.recently_used_fragment, container, false);

		binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

		return binding.getRoot();
	}
}
