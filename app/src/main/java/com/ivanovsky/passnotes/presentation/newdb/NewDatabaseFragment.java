package com.ivanovsky.passnotes.presentation.newdb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.presentation.core.BaseFragment;
import com.ivanovsky.passnotes.presentation.core.validation.BaseValidation;
import com.ivanovsky.passnotes.presentation.core.validation.IdenticalContentValidation;
import com.ivanovsky.passnotes.presentation.core.validation.NotEmptyValidation;
import com.ivanovsky.passnotes.presentation.core.validation.PatternValidation;
import com.ivanovsky.passnotes.presentation.core.validation.Validator;
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity;
import com.ivanovsky.passnotes.presentation.storagelist.Mode;
import com.ivanovsky.passnotes.presentation.storagelist.StorageListActivity;

import java.util.regex.Pattern;

import static com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput;

public class NewDatabaseFragment extends BaseFragment implements NewDatabaseContract.View {

	private static final int REQUEST_CODE_PICK_STORAGE = 100;

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[\\w]{1,50}");
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("[\\w@#$!%^&+=]{4,20}");

	private NewDatabaseContract.Presenter presenter;
	private Menu menu;
	private View storageLayout;
	private TextView storageTypeTextView;
	private TextView storagePathTextView;
	private EditText filenameEditText;
	private EditText passwordEditText;
	private EditText confirmationEditText;

	public static NewDatabaseFragment newInstance() {
		return new NewDatabaseFragment();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		presenter.start();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.new_database_fragment, container, false);

		storageLayout = view.findViewById(R.id.storage_layout);
		storageTypeTextView = view.findViewById(R.id.storage_type);
		storagePathTextView = view.findViewById(R.id.storage_path);
		filenameEditText = view.findViewById(R.id.filename);
		passwordEditText = view.findViewById(R.id.password);
		confirmationEditText = view.findViewById(R.id.password_confirmation);

		storageLayout.setOnClickListener(v -> presenter.selectStorage());

		return view;
	}

	@Override
	public void setStorage(String type, String path) {
		storageTypeTextView.setText(type);
		storagePathTextView.setText(path);
	}

	@Override
	public void setPresenter(NewDatabaseContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		this.menu = menu;

		inflater.inflate(R.menu.base_done, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_done) {
			onDoneMenuClicked();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void setDoneButtonVisible(boolean visible) {
		if (menu != null) {
			MenuItem item = menu.findItem(R.id.menu_done);
			if (item != null) {
				item.setVisible(visible);
			}
		}
	}

	@Override
	public void showGroupsScreen() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.finish();

			startActivity(GroupsActivity.Companion.createStartIntent(getContext()));
		}
	}

	@Override
	public void showError(String message) {
		setErrorPanelTextAndState(message);
	}

	private void onDoneMenuClicked() {
		Validator validator = new Validator.Builder()
				.validation(new NotEmptyValidation.Builder()
						.withTarget(filenameEditText)
						.withTarget(passwordEditText)
						.withTarget(confirmationEditText)
						.withErrorMessage(R.string.empty_field)
						.withPriority(BaseValidation.PRIORITY_MAX)
						.abortOnError(true)
						.build())
				.validation(new PatternValidation.Builder()
						.withPattern(FILE_NAME_PATTERN)
						.withTarget(filenameEditText)
						.withErrorMessage(R.string.field_contains_illegal_character)
						.build())
				.validation(new PatternValidation.Builder()
						.withPattern(PASSWORD_PATTERN)
						.withTarget(passwordEditText)
						.withErrorMessage(R.string.field_contains_illegal_character)
						.abortOnError(true)
						.build())
				.validation(new IdenticalContentValidation.Builder()
						.withFirstTarget(passwordEditText)
						.withSecondTarget(confirmationEditText)
						.withErrorMessage(R.string.this_field_should_match_password)
						.build())
				.build();

		if (validator.validateAll()) {
			presenter.createNewDatabaseFile(getFilename(), getPassword());
		}
	}

	private String getFilename() {
		return filenameEditText.getText().toString().trim();
	}

	private String getPassword() {
		return passwordEditText.getText().toString().trim();
	}

	@Override
	public void hideKeyboard() {
		hideSoftInput(getActivity());
	}

	@Override
	public void showStorageScreen() {
		startActivityForResult(StorageListActivity.Companion.createStartIntent(getContext(),
				Mode.PICK_DIRECTORY), REQUEST_CODE_PICK_STORAGE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK
				&& requestCode == REQUEST_CODE_PICK_STORAGE
				&& data != null
				&& data.getExtras() != null) {
			FileDescriptor file = data.getExtras().getParcelable(StorageListActivity.EXTRA_RESULT);

			if (file != null) {
				presenter.onStorageSelected(file);
			}
		}
	}
}
