package com.ivanovsky.passnotes.presentation.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.GlobalSnackbarPresenter
import java.util.*

class GroupsContract {

    interface View : BaseView<Presenter> {
        fun setItems(items: List<GroupsAdapter.ListItem>)
        fun showNewGroupScreen(parentGroupUid: UUID)
        fun showNoteListScreen(group: Group)
        fun showNoteScreen(note: Note)
    }

    interface Presenter : BasePresenter, GlobalSnackbarPresenter {
        fun loadData()
        fun onListItemClicked(position: Int)
        fun onAddButtonClicked()
    }
}