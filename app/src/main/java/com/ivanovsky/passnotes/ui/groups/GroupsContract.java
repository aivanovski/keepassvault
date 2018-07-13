package com.ivanovsky.passnotes.ui.groups;

import com.ivanovsky.passnotes.data.safedb.model.Group;
import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

import java.util.List;

public class GroupsContract {

	interface View extends BaseView<Presenter> {
		void showGroups(List<Group> groups);
		void showNoItems();
		void showNewGroupScreen();
		void showUnlockScreenAndFinish();
	}

	interface Presenter extends BasePresenter {
		void loadData();
	}
}
