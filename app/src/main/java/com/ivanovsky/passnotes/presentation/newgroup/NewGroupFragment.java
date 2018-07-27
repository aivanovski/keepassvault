package com.ivanovsky.passnotes.presentation.newgroup;

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
import com.ivanovsky.passnotes.databinding.NewGroupFragmentBinding;
import com.ivanovsky.passnotes.presentation.core.BaseFragment;
import com.ivanovsky.passnotes.presentation.core.FragmentState;

import static com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput;

public class NewGroupFragment extends BaseFragment implements NewGroupContract.View {

	private NewGroupContract.Presenter presenter;
	private NewGroupFragmentBinding binding;

	public static NewGroupFragment newInstance() {
		return new NewGroupFragment();
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
	public void onPause() {
		super.onPause();
		presenter.stop();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.new_group_fragment, container, false);
		return binding.getRoot();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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

	private void onDoneMenuClicked() {
		String title = binding.groupTitle.getText().toString();
		presenter.createNewGroup(title);
	}

	@Override
	public void setPresenter(NewGroupContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showTitleEditTextError(CharSequence error) {
		binding.groupTitle.setError(error);
	}

	@Override
	public void finishScreen() {
		hideSoftInput(getActivity());
		getActivity().finish();
	}

	@Override
	public void showError(String error) {
		setErrorPanelText(error);
		setState(FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL);
	}
}
