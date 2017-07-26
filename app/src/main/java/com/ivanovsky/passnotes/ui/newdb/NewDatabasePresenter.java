package com.ivanovsky.passnotes.ui.newdb;

import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseContract.Presenter;

public class NewDatabasePresenter implements Presenter {

	private final NewDatabaseContract.View view;

	public NewDatabasePresenter(NewDatabaseContract.View view) {
		this.view = view;
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}
}
