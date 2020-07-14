package com.ivanovsky.passnotes.presentation.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.GlobalSnackbarPresenter
import java.util.*

object GroupsContract {

    interface View : BaseView<Presenter> {
        fun setItems(items: List<GroupsAdapter.ListItem>)
        fun showNewGroupScreen(parentGroupUid: UUID)
        fun showNewNoteScreen(parentGroupUid: UUID, template: Template?)
        fun showNoteListScreen(group: Group)
        fun showNoteScreen(note: Note)
        fun showNewEntryDialog(templates: List<Template>)
        fun showGroupActionsDialog(group: Group)
        fun showNoteActionsDialog(note: Note)
        fun showRemoveConfirmationDialog(group: Group?, note: Note?)
        fun showEditNoteScreen(note: Note)
        fun showEditGroupScreen(group: Group)
    }

    interface Presenter : BasePresenter, GlobalSnackbarPresenter {
        fun loadData()
        fun onListItemClicked(position: Int)
        fun onListItemLongClicked(position: Int)
        fun onAddButtonClicked()
        fun onCreateNewNoteClicked()
        fun onCreateNewGroupClicked()
        fun onCreateNewNoteFromTemplateClicked(template: Template)
        fun onEditGroupClicked(group: Group)
        fun onRemoveGroupClicked(group: Group)
        fun onEditNoteClicked(note: Note)
        fun onRemoveNoteClicked(note: Note)
        fun onRemoveConfirmed(group: Group?, note: Note?)
    }
}