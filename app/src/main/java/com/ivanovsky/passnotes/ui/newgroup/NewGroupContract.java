package com.ivanovsky.passnotes.ui.newgroup;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class NewGroupContract {

	interface View extends BaseView<Presenter> {
		void showTitleEditTextError(CharSequence error);
		void finishScreen();
		void showError(String error);
	}

	interface Presenter extends BasePresenter {
		void createNewGroup(String title);
	}
}
