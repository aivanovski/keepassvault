package com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.Date
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class ResolveConflictDialogViewModel(
    private val interactor: ResolveConflictDialogInteractor,
    private val errorInteractor: ErrorInteractor,
    private val dateFormatProvider: DateFormatProvider,
    private val resourceProvider: ResourceProvider,
    private val args: ResolveConflictDialogArgs
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.loading())
    val message = MutableLiveData(EMPTY)
    val dismissEvent = SingleLiveEvent<Unit>()

    fun start() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val getConflictResult = interactor.getSyncConflictInfo(args.file)
            if (getConflictResult.isSucceededOrDeferred) {
                onSyncConflictInfoLoaded(getConflictResult.getOrThrow())
            } else {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(getConflictResult.error)
                    )
                )
            }
        }
    }

    fun onCancelButtonClicked() {
        dismissEvent.call()
    }

    fun onLocalButtonClicked() {
        onResolveConflictConfirmed(RESOLVE_WITH_LOCAL_FILE)
    }

    fun onRemoteButtonClicked() {
        onResolveConflictConfirmed(RESOLVE_WITH_REMOTE_FILE)
    }

    private fun onResolveConflictConfirmed(resolutionStrategy: ConflictResolutionStrategy) {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val resolvedConflict = interactor.resolveConflict(args.file, resolutionStrategy)
            if (resolvedConflict.isSucceededOrDeferred) {
                dismissEvent.call()
            } else {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(resolvedConflict.error)
                    )
                )
            }
        }
    }

    private fun onSyncConflictInfoLoaded(info: SyncConflictInfo) {
        message.value = resourceProvider.getString(
            R.string.resolve_conflict_dialog_message,
            info.localFile.modified?.formatDateAndTime() ?: EMPTY,
            info.remoteFile.modified?.formatDateAndTime() ?: EMPTY
        )
        setScreenState(ScreenState.data())
    }

    private fun Long.formatDateAndTime(): String {
        val date = Date(this)

        val dateFormat = dateFormatProvider.getLongDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        return dateFormat.format(date) + " " + timeFormat.format(date)
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
    }

    class Factory(private val args: ResolveConflictDialogArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<ResolveConflictDialogViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}