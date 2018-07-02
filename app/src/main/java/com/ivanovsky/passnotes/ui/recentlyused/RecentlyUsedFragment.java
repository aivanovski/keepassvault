package com.ivanovsky.passnotes.ui.recentlyused;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.DbDescriptor;
import com.ivanovsky.passnotes.databinding.RecentlyUsedFragmentBinding;
import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseActivity;
import com.ivanovsky.passnotes.ui.notepads.NotepadsActivity;

import java.util.List;

public class RecentlyUsedFragment extends BaseFragment implements RecentlyUsedContract.View {

	private UsedFile selectedUsedFile;
	private ArrayAdapter<String> fileAdapter;
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
	public void onPause() {
		super.onPause();
		presenter.stop();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.recently_used_fragment, container, false);

		fileAdapter = new ArrayAdapter<>(getContext(),
				android.R.layout.simple_spinner_item);
		fileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.fileSpinner.setAdapter(fileAdapter);

		binding.fab.setOnClickListener(view -> showNewDatabaseScreen());
		binding.unlockBtn.setOnClickListener(view -> onUnlockButtonClicked());

		return binding.getRoot();
	}

	private void onUnlockButtonClicked() {
	}

	@Override
	protected int getContentContainerId() {
		return R.id.content;
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
		fileAdapter.clear();
		fileAdapter.addAll(createAdapterItems(files));

		binding.fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				onFileSelected(files.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		setState(FragmentState.DISPLAYING_DATA);
	}

	private void onFileSelected(UsedFile file) {
		selectedUsedFile = file;
	}

	@Override
	public void showNoItems() {
		setEmptyText(getString(R.string.no_files_to_open));
		setState(FragmentState.EMPTY);
	}

	@Override
	public void showNotepadsScreen(DbDescriptor descriptor) {
		startActivity(NotepadsActivity.createIntent(getContext(), descriptor));
	}

	@Override
	public void showNewDatabaseScreen() {
		startActivity(new Intent(getContext(), NewDatabaseActivity.class));
	}

	private List<String> createAdapterItems(List<UsedFile> files) {
		return Stream.of(files)
				.map(UsedFile::getFilePath)
				.collect(Collectors.toList());
	}
}
