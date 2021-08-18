package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel
import com.ivanovsky.passnotes.util.formatAccordingLocale

class NoteCellViewModel(
    model: NoteCellModel,
    private val eventProvider: EventProvider,
    localeProvider: LocaleProvider
) : BaseCellViewModel(model) {

    val title = model.note.title
    val description = formatDescription(model.note)
    val date = model.note.modified.formatAccordingLocale(localeProvider.getSystemLocale())

    private fun formatDescription(note: Note): String {
        val filteredProperties = PROPERTY_FILTER.apply(note.properties)
        return if (filteredProperties.isNotEmpty()) {
            filteredProperties[0].value ?: ""
        } else {
            ""
        }
    }

    fun onClicked() {
        eventProvider.send((CLICK_EVENT to model.id).toEvent())
    }

    fun onLongClicked() {
        eventProvider.send((LONG_CLICK_EVENT to model.id).toEvent())
    }

    companion object {
        val CLICK_EVENT = NoteCellViewModel::class.qualifiedName + "_clickEvent"
        val LONG_CLICK_EVENT = NoteCellViewModel::class.qualifiedName + "_longClickEvent"

        private val PROPERTY_FILTER = PropertyFilter.Builder()
            .filterByType(PropertyType.USER_NAME, PropertyType.URL, PropertyType.NOTES)
            .notEmpty()
            .sortedByType()
            .build()
    }
}