package com.ivanovsky.passnotes.ui.newdb;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.NewDatabaseFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;

public class NewDatabaseFragment extends BaseFragment implements NewDatabaseContract.View {

	private NewDatabaseContract.Presenter presenter;

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
		NewDatabaseFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.new_database_fragment, container, false);
		return binding.getRoot();
	}

	@Override
	public void setPresenter(NewDatabaseContract.Presenter presenter) {
		this.presenter = presenter;
	}
}
