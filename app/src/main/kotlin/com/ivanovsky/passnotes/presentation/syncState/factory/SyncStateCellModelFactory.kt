package com.ivanovsky.passnotes.presentation.syncState.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.isSyncInProgress
import com.ivanovsky.passnotes.presentation.syncState.model.ButtonAction
import com.ivanovsky.passnotes.presentation.syncState.model.SyncStateModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class SyncStateCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createLoadingState(): SyncStateModel {
        return SyncStateModel(
            message = resourceProvider.getString(
                R.string.text_with_dots,
                resourceProvider.getString(R.string.checking)
            ),
            detailsMessage = EMPTY,
            messageColor = resourceProvider.getColor(R.color.primary_text),
            isSyncIconVisible = true,
            isMessageDismissed = false,
            buttonAction = ButtonAction.NONE
        )
    }

    fun createHiddenState(): SyncStateModel {
        return SyncStateModel(
            message = EMPTY,
            detailsMessage = EMPTY,
            messageColor = resourceProvider.getColor(R.color.primary_text),
            isSyncIconVisible = false,
            isMessageDismissed = false
        )
    }

    fun createFromSyncState(
        syncState: SyncState,
        isForceShowMessage: Boolean = false
    ): SyncStateModel {
        val message = formatMessage(syncState)
        val action = getButtonAction(
            syncState = syncState,
            isForceShowMessage = isForceShowMessage
        )
        val isInProgress = (
            syncState.progress == SyncProgressStatus.DOWNLOADING ||
                syncState.progress == SyncProgressStatus.UPLOADING
            )
        val isHideMessage = (
            syncState.status == SyncStatus.NO_CHANGES ||
                syncState.status == SyncStatus.LOCAL_CHANGES
            )

        return SyncStateModel(
            message = when {
                isForceShowMessage || isInProgress -> message
                isHideMessage -> EMPTY
                else -> message
            },
            detailsMessage = formatDetailedMessage(syncState),
            messageColor = getMessageColor(syncState),
            isSyncIconVisible = isInProgress,
            isMessageDismissed = false,
            buttonAction = action
        )
    }

    private fun getMessageColor(syncState: SyncState): Int {
        val isError = (
            syncState.status == SyncStatus.ERROR ||
                syncState.status == SyncStatus.AUTH_ERROR ||
                syncState.status == SyncStatus.FILE_NOT_FOUND ||
                syncState.status == SyncStatus.CONFLICT
            )

        return if (!syncState.progress.isSyncInProgress() && isError) {
            resourceProvider.getColor(R.color.error_text)
        } else {
            resourceProvider.getColor(R.color.primary_text)
        }
    }

    private fun getButtonAction(
        syncState: SyncState,
        isForceShowMessage: Boolean
    ): ButtonAction {
        return when {
            syncState.progress.isSyncInProgress() -> {
                ButtonAction.NONE
            }

            isForceShowMessage && syncState.status == SyncStatus.NO_CHANGES -> {
                ButtonAction.DISMISS
            }

            syncState.status == SyncStatus.ERROR ||
                syncState.status == SyncStatus.AUTH_ERROR ||
                syncState.status == SyncStatus.FILE_NOT_FOUND -> {
                ButtonAction.DETAILS
            }

            syncState.status == SyncStatus.LOCAL_CHANGES_NO_NETWORK ||
                syncState.status == SyncStatus.NO_NETWORK -> {
                ButtonAction.DISMISS
            }

            syncState.status == SyncStatus.CONFLICT -> {
                ButtonAction.RESOLVE
            }

            else -> ButtonAction.NONE
        }
    }

    private fun formatDetailedMessage(state: SyncState): String {
        return when (state.status) {
            SyncStatus.AUTH_ERROR -> {
                resourceProvider.getString(R.string.sync_auth_error_remove_message)
            }

            SyncStatus.FILE_NOT_FOUND -> {
                resourceProvider.getString(R.string.sync_file_not_found_message)
            }

            SyncStatus.ERROR -> {
                resourceProvider.getString(R.string.sync_error_message)
            }

            else -> EMPTY
        }
    }

    private fun formatMessage(
        state: SyncState
    ): String {
        return when {
            state.progress == SyncProgressStatus.SYNCING -> {
                EMPTY
            }

            state.progress == SyncProgressStatus.DOWNLOADING -> {
                resourceProvider.getString(
                    R.string.text_with_dots,
                    resourceProvider.getString(R.string.downloading)
                )
            }

            state.progress == SyncProgressStatus.UPLOADING -> {
                resourceProvider.getString(
                    R.string.text_with_dots,
                    resourceProvider.getString(R.string.uploading)
                )
            }

            state.status == SyncStatus.NO_CHANGES -> {
                resourceProvider.getString(R.string.file_is_up_to_date)
            }

            state.status == SyncStatus.LOCAL_CHANGES -> {
                resourceProvider.getString(R.string.not_synchronized)
            }

            state.status == SyncStatus.REMOTE_CHANGES -> {
                EMPTY
            }

            state.status == SyncStatus.LOCAL_CHANGES_NO_NETWORK -> {
                resourceProvider.getString(R.string.not_synchronized_and_offline_mode)
            }

            state.status == SyncStatus.NO_NETWORK -> {
                resourceProvider.getString(R.string.offline_mode)
            }

            state.status == SyncStatus.ERROR -> {
                resourceProvider.getString(R.string.sync_error)
            }

            state.status == SyncStatus.AUTH_ERROR -> {
                resourceProvider.getString(R.string.sync_error)
            }

            state.status == SyncStatus.FILE_NOT_FOUND -> {
                resourceProvider.getString(R.string.sync_error)
            }

            state.status == SyncStatus.CONFLICT -> {
                resourceProvider.getString(R.string.conflict)
            }

            else -> {
                EMPTY
            }
        }
    }
}