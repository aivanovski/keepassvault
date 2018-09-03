package com.ivanovsky.passnotes.presentation.newdb;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.presentation.core.BasePresenter;
import com.ivanovsky.passnotes.presentation.core.BaseView;

class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
		void showError(String message);
		void setStorage(String type, String path);
		void setDoneButtonVisible(boolean visible);
		void showGroupsScreen();
		void hideKeyboard();
		void showStorageScreen();
	}

	interface Presenter extends BasePresenter {
		void createNewDatabaseFile(String filename, String password);
		void selectStorage();
		void onStorageSelected(FileDescriptor file);
	}
}
