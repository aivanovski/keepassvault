package com.ivanovsky.passnotes.ui.recentlyused;

import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

import java.io.File;
import java.util.List;

public class RecentlyUsedContract {

	interface View extends BaseView<Presenter> {
		void showRecentlyUsedFiles(List<UsedFile> files);
		void showNoItems();
		void showNotepadsScreen();
		void showNewDatabaseScreen();
		void showLoading();
		void showError(String message);
	}

	interface Presenter extends BasePresenter {
		void loadData();
		void onUnlockButtonClicked(String password, File dbFile);
	}
}