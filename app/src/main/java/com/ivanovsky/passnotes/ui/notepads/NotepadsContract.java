package com.ivanovsky.passnotes.ui.notepads;

import com.ivanovsky.passnotes.data.safedb.model.Notepad;
import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

import java.util.List;

public class NotepadsContract {

	interface View extends BaseView<Presenter> {
		void showNotepads(List<Notepad> notepads);
		void showNoItems();
	}

	interface Presenter extends BasePresenter {
		void loadData();
		void showNewNotepadScreen();
	}
}
