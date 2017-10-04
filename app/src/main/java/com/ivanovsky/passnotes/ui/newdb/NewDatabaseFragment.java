package com.ivanovsky.passnotes.ui.newdb;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.NewDatabaseFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;

public class NewDatabaseFragment extends BaseFragment implements NewDatabaseContract.View {

	private NewDatabaseContract.Presenter presenter;
	private NewDatabaseFragmentBinding binding;

	public static NewDatabaseFragment newInstance() {
		return new NewDatabaseFragment();
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
	public EditText getFilenameEditText() {
		return binding.filename;
	}

	@Override
	public EditText getPasswordEditText() {
		return binding.password;
	}

	@Override
	public EditText getPasswordConfirmationEditText() {
		return binding.passwordConfirmation;
	}

	@Override
	public void showHomeActivity() {
		getActivity().finish();
	}

	@Override
	public void askForPermission() {
	}
}
