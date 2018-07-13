package com.ivanovsky.passnotes.ui.unlock;

import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

import java.io.File;
import java.util.List;

public class UnlockContract {

	interface View extends BaseView<Presenter> {
		void showRecentlyUsedFiles(List<UsedFile> files);
		void showNoItems();
		void showGroupsScreen();
		void showNewDatabaseScreen();
		void showLoading();
		void showError(String message);
	}

	interface Presenter extends BasePresenter {
		void loadData();
		void onUnlockButtonClicked(String password, File dbFile);
	}
}