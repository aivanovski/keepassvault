package com.ivanovsky.passnotes.ui.newdb;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.NewDatabaseFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.validation.BaseValidation;
import com.ivanovsky.passnotes.ui.core.validation.IdenticalContentValidation;
import com.ivanovsky.passnotes.ui.core.validation.NotEmptyValidation;
import com.ivanovsky.passnotes.ui.core.validation.PatternValidation;
import com.ivanovsky.passnotes.ui.core.validation.Validator;

import java.util.regex.Pattern;

public class NewDatabaseFragment extends BaseFragment implements NewDatabaseContract.View {

	private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[\\w]{1,50}");
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("[\\w@#$!%^&+=]{4,20}");

	private NewDatabaseContract.Presenter presenter;
	private NewDatabaseFragmentBinding binding;

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
		binding = DataBindingUtil.inflate(inflater, R.layout.new_database_fragment, container, false);
		return binding.getRoot();
	}

	@Override
	public void setPresenter(NewDatabaseContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.new_database, menu);
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
	public void showHomeActivity() {
		getActivity().finish();
	}

	@Override
	public void askForPermission() {
	}

	@Override
	public void onDoneMenuClicked() {
		Validator validator = new Validator.Builder()
				.validation(new NotEmptyValidation.Builder()
						.withTarget(binding.filename)
						.withTarget(binding.password)
						.withTarget(binding.passwordConfirmation)
						.withErrorMessage(R.string.empty_field)
						.withPriority(BaseValidation.PRIORITY_MAX)
						.abortOnError(true)
						.build())
				.validation(new PatternValidation.Builder()
						.withPattern(FILE_NAME_PATTERN)
						.withTarget(binding.filename)
						.withErrorMessage(R.string.field_contains_illegal_character)
						.build())
				.validation(new PatternValidation.Builder()
						.withPattern(PASSWORD_PATTERN)
						.withTarget(binding.password)
						.withErrorMessage(R.string.field_contains_illegal_character)
						.abortOnError(true)
						.build())
				.validation(new IdenticalContentValidation.Builder()
						.withFirstTarget(binding.password)
						.withSecondTarget(binding.passwordConfirmation)
						.withErrorMessage(R.string.this_field_should_match_password)
						.build())
				.build();

		if (validator.validateAll()) {
			presenter.createNewDatabaseFile(getFilename(), getPassword());
		}
	}

	private String getFilename() {
		return binding.filename.getText().toString().trim();
	}

	private String getPassword() {
		return binding.password.getText().toString().trim();
	}
}
