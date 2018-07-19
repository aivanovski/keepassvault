package com.ivanovsky.passnotes.presentation.unlock;

import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.presentation.core.BasePresenter;
import com.ivanovsky.passnotes.presentation.core.BaseView;

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