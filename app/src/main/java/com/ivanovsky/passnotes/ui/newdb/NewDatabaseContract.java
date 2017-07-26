package com.ivanovsky.passnotes.ui.newdb;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
	}

	interface Presenter extends BasePresenter {
		void loadData();
	}
}
