package com.ivanovsky.passnotes.ui.newdb;

import android.content.Context;
import android.widget.EditText;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseContract.Presenter;

import java.util.regex.Pattern;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NewDatabasePresenter implements Presenter {

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[\\w]{1,50}");
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("[\\w@#$!%^&+=]{4,20}");

	private final NewDatabaseContract.View view;
	private final Context context;

	public NewDatabasePresenter(NewDatabaseContract.View view, Context context) {
		this.view = view;
		this.context = context;
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public boolean onOptionsItemSelected(int menuItemId) {
		if (menuItemId == R.id.menu_done) {
			onDoneMenuClicked();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onDoneMenuClicked() {
		if (validateFieldsData()) {
			Observable.fromCallable(this::createNewDatabaseFile)
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::onNewDatabaseCreated);

			view.showHomeActivity();
		}
	}

	private Boolean createNewDatabaseFile() {

		return null;
	}

	private void onNewDatabaseCreated(Boolean created) {

	}

	@Override
	public void onPermissionResult(boolean granted) {

	}

	@Override
	public boolean validateFieldsData() {
		boolean result = true;

		EditText filenameEditText = view.getFilenameEditText();
		EditText passwordEditText = view.getPasswordEditText();
		EditText passwordConfirmationEditText = view.getPasswordConfirmationEditText();

		String filename = filenameEditText.getText().toString();
		if (!isValidFilenameText(filename)) {
			filenameEditText.setError(context.getString(R.string.field_contains_illegal_character));

			result = false;
		} else {
			filenameEditText.setError(null);
		}

		String password = passwordEditText.getText().toString();
		if (!isValidPassword(password)) {
			passwordEditText.setError(context.getString(R.string.field_contains_illegal_character));

			result = false;
		} else {
			passwordEditText.setError(null);
		}

		String confirmedPassword = passwordConfirmationEditText.getText().toString();
		if (!confirmedPassword.equals(password)) {
			passwordConfirmationEditText.setError(context.getString(R.string.you_entered_two_different_passwords_please_try_again));

			result = false;
		} else {
			passwordConfirmationEditText.setError(null);
		}

		return result;
	}

	private boolean isValidFilenameText(String filename) {
		return FILE_NAME_PATTERN.matcher(filename).matches();
	}

	private boolean isValidPassword(String password) {
		return PASSWORD_PATTERN.matcher(password).matches();
	}
}
