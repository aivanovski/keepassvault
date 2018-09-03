package com.ivanovsky.passnotes.presentation.unlock;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.ivanovsky.passnotes.BuildConfig;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.UsedFile;
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
	private UnlockContract.Presenter presenter;
	private List<PasswordRule> passwordRules;
	private Spinner fileSpinner;
	private FloatingActionButton fab;
	private EditText passwordEditText;

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
		View view = inflater.inflate(R.layout.unlock_fragment, container, false);

		fileSpinner = view.findViewById(R.id.file_spinner);
		fab = view.findViewById(R.id.fab);
		passwordEditText = view.findViewById(R.id.password);
		View unlockButton = view.findViewById(R.id.unlock_button);

		fileAdapter = new FileSpinnerAdapter(getContext());

		fileSpinner.setAdapter(fileAdapter);

		fab.setOnClickListener(v -> showNewDatabaseScreen());
		unlockButton.setOnClickListener(v -> onUnlockButtonClicked());

		presenter.getRecentlyUsedFilesData().observe(this, this::showRecentlyUsedFiles);
		presenter.getScreenStateData().observe(this, this::setScreenState);
		presenter.getShowGroupsScreenAction().observe(this, obj -> showGroupsScreen());
		presenter.getShowNewDatabaseScreenAction().observe(this, obj -> showNewDatabaseScreen());
		presenter.getHideKeyboardAction().observe(this, obj -> hideKeyboard());

		return view;
	}

	private void onUnlockButtonClicked() {
		String password = passwordEditText.getText().toString().trim();

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
				fab.setVisibility(View.VISIBLE);
				break;

			case LOADING:
			case ERROR:
				fab.setVisibility(View.GONE);
				break;
		}
	}

	@Override
	public void setPresenter(UnlockContract.Presenter presenter) {
		this.presenter = presenter;
	}

	private void showRecentlyUsedFiles(List<UsedFile> files) {
		selectedUsedFile = files.get(0);

		fileAdapter.setItem(createAdapterItems(files));
		fileAdapter.notifyDataSetChanged();

		fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				onFileSelected(files.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
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
						passwordEditText.setText(rule.password);
						break;
					}
				}
			}
		}
	}

	private void showGroupsScreen() {
		startActivity(GroupsActivity.Companion.createStartIntent(getContext()));
	}

	private void showNewDatabaseScreen() {
		startActivity(new Intent(getContext(), NewDatabaseActivity.class));
	}

	private void hideKeyboard() {
		hideSoftInput(getActivity());
	}

	private static class PasswordRule {
		private Pattern pattern;
		private String password;
	}
}
