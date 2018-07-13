package com.ivanovsky.passnotes.ui.groups;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.safedb.model.Group;
import com.ivanovsky.passnotes.databinding.GroupsFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.newgroup.NewGroupActivity;
import com.ivanovsky.passnotes.ui.groups.GroupsAdapter.ListItem;
import com.ivanovsky.passnotes.ui.notes.NotesActivity;
import com.ivanovsky.passnotes.ui.unlock.UnlockActivity;

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends BaseFragment implements GroupsContract.View {

	private GroupsContract.Presenter presenter;
	private GroupsFragmentBinding binding;
	private GroupsAdapter adapter;

	public static GroupsFragment newInstance() {
		return new GroupsFragment();
	}

	@Override
	public void onResume() {
		super.onResume();
		presenter.start();
	}

	@Override
	public void onPause() {
		super.onPause();
		presenter.stop();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.groups_fragment, container, false);

		GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);

		binding.recyclerView.setLayoutManager(layoutManager);
		binding.recyclerView.setAdapter(adapter = new GroupsAdapter(getContext()));

		binding.fab.setOnClickListener(view -> showNewGroupScreen());

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
	public void setPresenter(GroupsContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showGroups(List<Group> groups) {
		adapter.setItems(createAdapterItems(groups));
		adapter.setOnItemClickListener(position -> onGroupClicked(groups.get(position)));
		setState(FragmentState.DISPLAYING_DATA);
	}

	private List<ListItem> createAdapterItems(List<Group> groups) {
		List<ListItem> result = new ArrayList<>();

		for (Group group : groups) {
			result.add(new ListItem(group.getTitle()));
		}

		return result;
	}

	private void onGroupClicked(Group group) {
		presenter.onGroupClicked(group);
	}

	@Override
	public void showNoItems() {
		setEmptyText(getString(R.string.no_items_to_show));
		setState(FragmentState.EMPTY);
	}

	@Override
	public void showNewGroupScreen() {
		startActivity(NewGroupActivity.createStartIntent(getContext()));
	}

	@Override
	public void showUnlockScreenAndFinish() {
		startActivity(UnlockActivity.createStartIntent(getContext()));

		getActivity().finish();
	}

	@Override
	public void showNotesScreen(Group group) {
		startActivity(NotesActivity.Companion.createStartIntent(getContext()));
	}
}
