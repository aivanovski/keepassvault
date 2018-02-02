package com.ivanovsky.passnotes.ui.newnotepad;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class NewNotepadContract {

	interface View extends BaseView<Presenter> {
		void showTitleEditTextError(CharSequence error);
		void finishScreen();
		void showError(String error);
	}

	interface Presenter extends BasePresenter {
		void createNewNotepad(String notepadTitle);
	}
}
