package com.ivanovsky.passnotes.presentation.unlock;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.ivanovsky.passnotes.BuildConfig;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.databinding.UnlockFragmentBinding;
import com.ivanovsky.passnotes.presentation.core.BaseFragment;
import com.ivanovsky.passnotes.presentation.core.FragmentState;
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.ivanovsky.passnotes.util.FileUtils.getFileNameFromPath;
import static com.ivanovsky.passnotes.util.FileUtils.getFileNameWithoutExtensionFromPath;
import static com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput;

public class UnlockFragment extends BaseFragment implements UnlockContract.View {

	private UsedFile selectedUsedFile;
	private FileSpinnerAdapter fileAdapter;
	private UnlockFragmentBinding binding;
	private UnlockContract.Presenter presenter;
	private List<PasswordRule> passwordRules;

	public static UnlockFragment newInstance() {
		return new UnlockFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (BuildConfig.DEBUG) {
			passwordRules = compileFileNamePatterns();
		}
	}

	private List<PasswordRule> compileFileNamePatterns() {
		List<PasswordRule> rules = new ArrayList<>();

		if (BuildConfig.DEBUG_FILE_NAME_PATTERNS != null
				&& BuildConfig.DEBUG_PASSWORDS != null) {
			for (int idx = 0; idx < BuildConfig.DEBUG_FILE_NAME_PATTERNS.length; idx++) {
				String fileNamePattern = BuildConfig.DEBUG_FILE_NAME_PATTERNS[idx];
				String password = BuildConfig.DEBUG_PASSWORDS[idx];

				PasswordRule rule = new PasswordRule();

				rule.pattern = Pattern.compile(fileNamePattern);
				rule.password = password;

				rules.add(rule);
			}
		}

		return  rules;
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
		binding = DataBindingUtil.inflate(inflater, R.layout.unlock_fragment, container, false);

		fileAdapter = new FileSpinnerAdapter(getContext());

		binding.fileSpinner.setAdapter(fileAdapter);

		binding.fab.setOnClickListener(view -> showNewDatabaseScreen());
		binding.unlockBtn.setOnClickListener(view -> onUnlockButtonClicked());

		return binding.getRoot();
	}

	private void onUnlockButtonClicked() {
		String password = binding.password.getText().toString().trim();

		File dbFile = new File(selectedUsedFile.getFilePath());
		presenter.onUnlockButtonClicked(password, dbFile);
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
	public void setPresenter(UnlockContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showRecentlyUsedFiles(List<UsedFile> files) {
		selectedUsedFile = files.get(0);

		fileAdapter.setItem(createAdapterItems(files));
		fileAdapter.notifyDataSetChanged();

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

	private List<FileSpinnerAdapter.Item> createAdapterItems(List<UsedFile> files) {
		List<FileSpinnerAdapter.Item> items = new ArrayList<>();

		for (UsedFile file : files) {
			String path = file.getFilePath();
			String filename = getFileNameFromPath(path);

			items.add(new FileSpinnerAdapter.Item(filename, path));
		}

		return items;
	}

	private void onFileSelected(UsedFile file) {
		selectedUsedFile = file;

		if (BuildConfig.DEBUG) {
			String fileName = getFileNameWithoutExtensionFromPath(file.getFilePath());
			if (fileName != null) {
				for (PasswordRule rule : passwordRules) {
					if (rule.pattern.matcher(fileName).matches()) {
						binding.password.setText(rule.password);
						break;
					}
				}
			}
		}
	}

	@Override
	public void showNoItems() {
		setEmptyText(getString(R.string.no_files_to_open));
		setState(FragmentState.EMPTY);
	}

	@Override
	public void showGroupsScreen() {
		startActivity(GroupsActivity.Companion.createStartIntent(getContext()));
	}

	@Override
	public void showNewDatabaseScreen() {
		startActivity(new Intent(getContext(), NewDatabaseActivity.class));
	}

	@Override
	public void showError(String message) {
		setErrorPanelTextAndState(message);
	}

	@Override
	public void hideKeyboard() {
		hideSoftInput(getActivity());
	}

	private static class PasswordRule {
		private Pattern pattern;
		private String password;
	}
}
