package com.ivanovsky.passnotes.ui.groups

import com.ivanovsky.passnotes.data.safedb.model.Group
import com.ivanovsky.passnotes.ui.core.BasePresenter
import com.ivanovsky.passnotes.ui.core.BaseView

class GroupsContract {

	interface View: BaseView<Presenter> {
		fun showGroups(groups: List<Group>)
		fun showNoItems()
		fun showNewGroupScreen()
		fun showUnlockScreenAndFinish()
		fun showNotesScreen(group: Group)
	}

	interface Presenter: BasePresenter {
		fun loadData()
		fun onGroupClicked(group: Group)
	}
}