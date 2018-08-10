package com.ivanovsky.passnotes.presentation.newdb;

import com.ivanovsky.passnotes.data.entity.DatabaseDescriptor;
import com.ivanovsky.passnotes.presentation.core.BasePresenter;
import com.ivanovsky.passnotes.presentation.core.BaseView;

public class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
		void showError(String message);
		void setDoneButtonVisible(boolean visible);
		void showGroupsScreen(DatabaseDescriptor descriptor);
		void hideKeyboard();
		void showStorageScreen();
	}

	interface Presenter extends BasePresenter {
		void createNewDatabaseFile(String filename, String password);
		void selectStorage();
	}
}
