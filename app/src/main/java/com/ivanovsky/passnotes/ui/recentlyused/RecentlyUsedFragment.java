package com.ivanovsky.passnotes.ui.recentlyused;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.RecentlyUsedFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.adapter.TwoLineTwoTextAdapter;

public class RecentlyUsedFragment extends BaseFragment {

	private TwoLineTwoTextAdapter adapter;

	public static RecentlyUsedFragment newInstance() {
		return new RecentlyUsedFragment();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		RecentlyUsedFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.recently_used_fragment, container, false);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());

		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.addItemDecoration(dividerItemDecoration);
		binding.recyclerView.setAdapter(adapter = new TwoLineTwoTextAdapter(getContext()));

		return binding.getRoot();
	}
}
