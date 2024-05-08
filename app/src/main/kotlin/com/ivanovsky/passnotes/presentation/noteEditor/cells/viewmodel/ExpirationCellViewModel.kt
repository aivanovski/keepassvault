package com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.ExpirationCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.Date

class ExpirationCellViewModel(
    initModel: ExpirationCellModel,
    private val eventProvider: EventProvider,
    private val dateFormatProvider: DateFormatProvider
) : BaseMutableCellViewModel<ExpirationCellModel>(initModel) {

    val isEnabled = MutableLiveData(initModel.isEnabled)
    val date = MutableLiveData(formatDate(initModel.timestamp.timeInMillis))
    val time = MutableLiveData(formatTime(initModel.timestamp.timeInMillis))

    override fun setModel(newModel: ExpirationCellModel) {
        super.setModel(newModel)
        isEnabled.value = newModel.isEnabled
        date.value = formatDate(newModel.timestamp.timeInMillis)
        time.value = formatTime(newModel.timestamp.timeInMillis)
    }

    fun onDateClicked() {
        eventProvider.send((DATE_CLICK_EVENT to model.id).toEvent())
    }

    fun onTimeClicked() {
        eventProvider.send((TIME_CLICK_EVENT to model.id).toEvent())
    }

    fun onEnabledStateChanged(isEnabled: Boolean) {
        val model = mutableModel as ExpirationCellModel
        setModel(model.copy(isEnabled = isEnabled))
    }

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp != null) {
            dateFormatProvider.getShortDateFormat().format(Date(timestamp))
        } else {
            EMPTY
        }
    }

    private fun formatTime(timestamp: Long?): String {
        return if (timestamp != null) {
            dateFormatProvider.getTimeFormat().format(Date(timestamp))
        } else {
            EMPTY
        }
    }

    companion object {
        val DATE_CLICK_EVENT = ExpirationCellViewModel::class.simpleName + "_dateClickEvent"
        val TIME_CLICK_EVENT = ExpirationCellViewModel::class.simpleName + "_timeClickEvent"
    }
}