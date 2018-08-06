package com.ivanovsky.passnotes.presentation.newdb;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.DatabaseDescriptor;
import com.ivanovsky.passnotes.presentation.core.BaseFragment;
import com.ivanovsky.passnotes.presentation.core.validation.BaseValidation;
import com.ivanovsky.passnotes.presentation.core.validation.IdenticalContentValidation;
import com.ivanovsky.passnotes.presentation.core.validation.NotEmptyValidation;
import com.ivanovsky.passnotes.presentation.core.validation.PatternValidation;
import com.ivanovsky.passnotes.presentation.core.validation.Validator;
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity;

import java.util.regex.Pattern;

import static com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput;

public class NewDatabaseFragment extends BaseFragment implements NewDatabaseContract.View {

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[\\w]{1,50}");
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("[\\w@#$!%^&+=]{4,20}");

	private NewDatabaseContract.Presenter presenter;
	private Menu menu;
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

		filenameEditText = view.findViewById(R.id.filename);
		passwordEditText = view.findViewById(R.id.password);
		confirmationEditText = view.findViewById(R.id.password_confirmation);

		return view;
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
	public void showGroupsScreen(DatabaseDescriptor dbDescriptor) {
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
}
