package com.ivanovsky.passnotes.ui.recentlyused;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.RecentlyUsedFragmentBinding;
import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.core.adapter.TwoLineTwoTextAdapter;

import java.util.ArrayList;
import java.util.List;

public class RecentlyUsedFragment extends BaseFragment implements RecentlyUsedContract.View {

	private TwoLineTwoTextAdapter adapter;
	private RecentlyUsedFragmentBinding binding;
	private RecentlyUsedContract.Presenter presenter;

	public static RecentlyUsedFragment newInstance() {
		return new RecentlyUsedFragment();
	}

	@Override
	public void onResume() {
		super.onResume();
		presenter.start();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.recently_used_fragment, container, false);

		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());

		adapter = new TwoLineTwoTextAdapter(getContext());

		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.addItemDecoration(dividerItemDecoration);
		binding.recyclerView.setAdapter(adapter);

		binding.fab.setOnClickListener(view -> presenter.showNewDatabaseScreen());

		return binding.getRoot();
	}

	@Override
	protected int getContentContainerId() {
		return R.id.recycler_view;
	}

	@Override
	protected void onStateChanged(FragmentState oldState, FragmentState newState) {
		switch (newState) {
			case EMPTY:
			case DISPLAYING_DATA_WITH_ERROR_PANEL:
			case DISPLAYING_DATA:
				binding.fab.setVisibility(View.VISIBLE);
				break;

			case LOADING:
			case ERROR:
				binding.fab.setVisibility(View.GONE);
				break;
		}
	}

	@Override
	public void setPresenter(RecentlyUsedContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showRecentlyUsedFiles(List<UsedFile> files) {
		adapter.setItems(createAdapterItems(files));
		setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public void showNoItems() {
		setEmptyText(getString(R.string.no_files_to_open));
		setState(FragmentState.EMPTY);
	}

	private List<TwoLineTwoTextAdapter.ListItem> createAdapterItems(List<UsedFile> files) {
		List<TwoLineTwoTextAdapter.ListItem> result = new ArrayList<>();

		for (UsedFile file : files) {
			String filePath = file.getFilePath();

			if (TextUtils.isEmpty(filePath)) {
				continue;
			}

			String fileName = getFileNameFromPath(filePath);

			result.add(new TwoLineTwoTextAdapter.ListItem(fileName, filePath));
		}

		return result;
	}

	private String getFileNameFromPath(String filePath) {
		if (filePath == null) {
			return null;
		}

		String fileName = "";

		int idx = filePath.lastIndexOf("/");
		if (idx > 0
				&& idx + 1 < filePath.length()) {
			fileName = filePath.substring(idx + 1, filePath.length());
		}

		return fileName;
	}
}
