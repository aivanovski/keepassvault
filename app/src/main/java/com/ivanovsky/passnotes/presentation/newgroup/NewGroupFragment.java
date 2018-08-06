package com.ivanovsky.passnotes.presentation.newgroup;

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
import com.ivanovsky.passnotes.presentation.core.BaseFragment;

import static com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput;

public class NewGroupFragment extends BaseFragment implements NewGroupContract.View {

	private NewGroupContract.Presenter presenter;
	private Menu menu;
	private EditText titleEditText;

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
		View view = inflater.inflate(R.layout.new_group_fragment, container, false);

		titleEditText = view.findViewById(R.id.group_title);

		return view;
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

	private void onDoneMenuClicked() {
		String title = titleEditText.getText().toString();
		presenter.createNewGroup(title);
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
	public void setPresenter(NewGroupContract.Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showTitleEditTextError(CharSequence error) {
		titleEditText.setError(error);
	}

	@Override
	public void finishScreen() {
		getActivity().finish();
	}

	@Override
	public void showError(String error) {
		setErrorPanelTextAndState(error);
	}

	@Override
	public void hideKeyboard() {
		hideSoftInput(getActivity());
	}
}
