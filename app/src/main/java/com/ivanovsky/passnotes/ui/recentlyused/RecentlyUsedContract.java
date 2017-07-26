package com.ivanovsky.passnotes.ui.recentlyused;

import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

import java.util.List;

public class RecentlyUsedContract {

	interface View extends BaseView<Presenter> {
		void setRecentlyUsedFiles(List<UsedFile> files);
	}

	interface Presenter extends BasePresenter {
		void loadData();
		void showNewDatabaseScreen();
	}
}
