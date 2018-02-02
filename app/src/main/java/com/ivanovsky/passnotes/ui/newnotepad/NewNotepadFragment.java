package com.ivanovsky.passnotes.ui.newnotepad;

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
import com.ivanovsky.passnotes.databinding.NewNotepadFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;
import com.ivanovsky.passnotes.ui.core.FragmentState;

public class NewNotepadFragment extends BaseFragment implements NewNotepadContract.View {

	private NewNotepadContract.Presenter presenter;
	private NewNotepadFragmentBinding binding;
	private Menu menu;

	public static NewNotepadFragment newInstance() {
		return new NewNotepadFragment();
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
		binding = DataBindingUtil.inflate(inflater, R.layout.new_notepad_fragment, container, false);
		return binding.getRoot();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		this.menu = menu;

		inflater.inflate(R.menu.done, menu);
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
		String title = binding.notepadTitle.getText().toString();
		presenter.createNewNotepad(title);
	}

	@Override
	public void setPresenter(NewNotepadContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showTitleEditTextError(CharSequence error) {
		binding.notepadTitle.setError(error);
	}

	@Override
	public void finishScreen() {
		getActivity().finish();
	}

	@Override
	public void showError(String error) {
		setErrorPanelText(error);
		setState(FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL);
	}
}
