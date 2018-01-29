package com.ivanovsky.passnotes.ui.newdb;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
		void onDoneMenuClicked();
		void showError(String message);
		void setDoneButtonVisible(boolean visible);
		void showNotepadsScreen(String dbName);
	}

	interface Presenter extends BasePresenter {
		void createNewDatabaseFile(String filename, String password);
	}
}
