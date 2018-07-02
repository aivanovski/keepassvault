package com.ivanovsky.passnotes.ui.newdb;

import com.ivanovsky.passnotes.data.DbDescriptor;
import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

import java.io.File;

public class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
		void showError(String message);
		void setDoneButtonVisible(boolean visible);
		void showNotepadsScreen(DbDescriptor descriptor);
	}

	interface Presenter extends BasePresenter {
		void createNewDatabaseFile(String filename, String password);
	}
}
