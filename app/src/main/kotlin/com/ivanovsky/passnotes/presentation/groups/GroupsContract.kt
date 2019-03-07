package com.ivanovsky.passnotes.presentation.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.GlobalSnackbarPresenter

class GroupsContract {

	interface View: BaseView<Presenter> {
		fun showGroups(groupsAndCounts: List<Pair<Group, Int>>)
		fun showNoItems()
		fun showNewGroupScreen()
		fun showUnlockScreenAndFinish()
		fun showNotesScreen(group: Group)
		fun showError(message: String)
	}

	interface Presenter: BasePresenter, GlobalSnackbarPresenter {
		fun loadData()
		fun onGroupClicked(group: Group)
	}
}