package com.ivanovsky.passnotes.presentation.newgroup;

import com.ivanovsky.passnotes.presentation.core.BasePresenter;
import com.ivanovsky.passnotes.presentation.core.BaseView;

class NewGroupContract {

	interface View extends BaseView<Presenter> {
		void showTitleEditTextError(CharSequence error);
		void finishScreen();
		void showError(String error);
	}

	interface Presenter extends BasePresenter {
		void createNewGroup(String title);
	}
}
