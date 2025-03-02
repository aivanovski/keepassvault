package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel

class GroupCellViewModel(
    override val model: GroupCellModel,
    private val eventProvider: EventProvider,
    private val resourceProvider: ResourceProvider
) : BaseCellViewModel(model) {

    val title = model.group.title
    val description = formatCountsForGroup(model.noteCount, model.childGroupCount)
    val isDescriptionVisible = description.isNotEmpty()

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    private fun formatCountsForGroup(noteCount: Int, childGroupCount: Int): String {
        return if (noteCount == 0 && childGroupCount == 0) {
            ""
        } else if (noteCount > 0 && childGroupCount == 0) {
            resourceProvider.getString(R.string.entries_with_count, noteCount)
        } else if (noteCount == 0 && childGroupCount > 0) {
            resourceProvider.getString(R.string.groups_with_count, childGroupCount)
        } else {
            resourceProvider.getString(
                R.string.groups_and_entries_with_count,
                childGroupCount,
                noteCount
            )
        }
    }

    companion object {

        val CLICK_EVENT = GroupCellViewModel::class.qualifiedName + "_clickEvent"
        val LONG_CLICK_EVENT = GroupCellViewModel::class.qualifiedName + "_longClickEvent"
    }
}