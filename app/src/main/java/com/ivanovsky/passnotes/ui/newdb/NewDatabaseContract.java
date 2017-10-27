package com.ivanovsky.passnotes.ui.newdb;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
		void showHomeActivity();
		void askForPermission();
		void onDoneMenuClicked();
	}

	interface Presenter extends BasePresenter {
		void onPermissionResult(boolean granted);
		void createNewDatabaseFile(String filename, String password);
	}
}
