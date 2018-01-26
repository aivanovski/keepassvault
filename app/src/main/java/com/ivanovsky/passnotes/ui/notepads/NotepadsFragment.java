package com.ivanovsky.passnotes.ui.notepads;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;
import com.ivanovsky.passnotes.databinding.NotepadsFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.notepads.NotepadsAdapter.ListItem;

import java.util.ArrayList;
import java.util.List;

public class NotepadsFragment extends BaseFragment implements NotepadsContract.View {

	private NotepadsContract.Presenter presenter;
	private NotepadsFragmentBinding binding;
	private NotepadsAdapter adapter;

	public static NotepadsFragment newInstance() {
		return new NotepadsFragment();
	}

	@Override
	public void onResume() {
		super.onResume();
		presenter.start();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.notepads_fragment, container, false);

		GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);

		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.setAdapter(adapter = new NotepadsAdapter(getContext()));

		binding.fab.setOnClickListener(view -> presenter.showNewNotepadScreen());

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
	public void setPresenter(NotepadsContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showNotepads(List<Notepad> notepads) {
		adapter.setItems(createAdapterItems(notepads));
		setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public void showNoItems() {
		setEmptyText(getString(R.string.no_items_to_show));
		setState(FragmentState.EMPTY);
	}

	private List<ListItem> createAdapterItems(List<Notepad> notepads) {
		List<ListItem> result = new ArrayList<>();

		for (Notepad notepad : notepads) {
			result.add(new ListItem(notepad.getTitle()));
		}

		return result;
	}
}
