package com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.MergeFiles
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution.DOWNLOAD_REMOTE_FILE
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution.UPLOAD_LOCAL_FILE
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.DiffViewerScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenVisibilityHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerMode
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerScreenArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.Date
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class ResolveConflictDialogViewModel(
    private val interactor: ResolveConflictDialogInteractor,
    private val dateFormatProvider: DateFormatProvider,
    private val resourceProvider: ResourceProvider,
    private val router: Router,
    private val args: ResolveConflictDialogArgs
) : BaseScreenViewModel(
    initialState = ScreenState.loading()
) {

    val screenVisibilityHandler = DefaultScreenVisibilityHandler()
    val message = MutableLiveData(EMPTY)
    val dismissEvent = SingleLiveEvent<Unit>()
    val isMergeButtonVisible = MutableLiveData(false)
    val showSnackbarMessageEvent = SingleLiveEvent<String>()

    fun start() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            interactor.getSyncConflictInfo(args.file).fold(
                ifLeft = { error -> setErrorState(error) },
                ifRight = { info -> onSyncConflictInfoLoaded(info) }
            )
        }
    }

    fun onCancelButtonClicked() {
        dismissEvent.call(Unit)
    }

    fun onMergeButtonClicked() {
        if (!interactor.isDatabaseOpened()) {
            showSnackbarMessageEvent.value = resourceProvider.getString(
                R.string.unlock_database_error_message
            )
            return
        }

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            either {
                val key = interactor.getOpenedDatabaseKey().bind()
                val files = interactor.getMergeFiles(args.file).bind()

                key to files
            }.fold(
                ifLeft = { error -> setErrorState(error) },
                ifRight = { (key, files) ->
                    navigateToDiffViewer(key, files)
                    dismissEvent.call(Unit)
                }
            )
        }
    }

    private fun navigateToDiffViewer(
        key: EncryptedDatabaseKey,
        files: MergeFiles
    ) {
        router.navigateTo(
            DiffViewerScreen(
                DiffViewerScreenArgs(
                    mode = DiffViewerMode.Merge(
                        key = key,
                        base = files.base,
                        local = files.local,
                        remote = files.remote,
                        output = files.output
                    ),
                    isHoldDatabaseInteraction = true
                )
            )
        )
    }

    fun onLocalButtonClicked() {
        onResolveConflictConfirmed(UPLOAD_LOCAL_FILE)
    }

    fun onRemoteButtonClicked() {
        onResolveConflictConfirmed(DOWNLOAD_REMOTE_FILE)
    }

    private fun onResolveConflictConfirmed(requestedResolution: RequestedSyncResolution) {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            interactor.resolveConflict(args.file, requestedResolution).fold(
                ifLeft = { error -> setErrorState(error) },
                ifRight = { dismissEvent.call(Unit) }
            )
        }
    }

    private fun onSyncConflictInfoLoaded(info: SyncConflictInfo) {
        message.value = resourceProvider.getString(
            R.string.resolve_conflict_dialog_message,
            info.localFile.modified?.formatDateAndTime() ?: EMPTY,
            info.remoteFile.modified?.formatDateAndTime() ?: EMPTY
        )
        isMergeButtonVisible.value = info.isMergeAvailable
        setScreenState(ScreenState.data())
    }

    private fun Long.formatDateAndTime(): String {
        val date = Date(this)

        val dateFormat = dateFormatProvider.getLongDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        return dateFormat.format(date) + " " + timeFormat.format(date)
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