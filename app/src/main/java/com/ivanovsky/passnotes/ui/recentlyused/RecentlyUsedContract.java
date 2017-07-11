package com.ivanovsky.passnotes.ui.recentlyused;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class RecentlyUsedContract {

	interface View extends BaseView {
		void displayLoadingProgress();
		void displayRecentlyUsedFiles();
		void displayEmptyText();
	}

	interface Presenter extends BasePresenter {
		void load
	}
}
