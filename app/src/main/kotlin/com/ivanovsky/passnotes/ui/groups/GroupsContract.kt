package com.ivanovsky.passnotes.ui.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

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