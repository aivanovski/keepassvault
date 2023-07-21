package com.ivanovsky.passnotes.presentation.syncState.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.isSyncInProgress
import com.ivanovsky.passnotes.presentation.syncState.model.ButtonAction
import com.ivanovsky.passnotes.presentation.syncState.model.SyncStateModel
import com.ivanovsky.passnotes.util.StringUtils

class SyncStateCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createLoadingState(): SyncStateModel {
        return SyncStateModel(
            message = StringUtils.EMPTY,
            messageColor = resourceProvider.getColor(R.color.primary_text),
            isProgressVisible = true,
            isMessageDismissed = true,
            buttonAction = ButtonAction.NONE
        )
    }

    fun createHiddenState(): SyncStateModel {
        return SyncStateModel(
            message = StringUtils.EMPTY,
            messageColor = resourceProvider.getColor(R.color.primary_text),
            isProgressVisible = false,
            isMessageDismissed = false
        )
    }

    fun createFromSyncState(
        syncState: SyncState,
        isForceMessage: Boolean = false
    ): SyncStateModel {
        val message = formatSyncStateMessage(syncState)
        val action = getButtonAction(syncState)
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
                isForceMessage || isInProgress -> message
                isHideMessage -> StringUtils.EMPTY
                else -> message
            },
            messageColor = getMessageColor(syncState),
            isProgressVisible = false,
            isMessageDismissed = false,
            buttonAction = if (isForceMessage && syncState.status == SyncStatus.NO_CHANGES) {
                ButtonAction.DISMISS
            } else {
                action
            }
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

    private fun getButtonAction(state: SyncState): ButtonAction {
        return when {
            state.progress.isSyncInProgress() -> ButtonAction.NONE
            state.status == SyncStatus.CONFLICT -> ButtonAction.RESOLVE
            else -> ButtonAction.NONE
        }
    }

    private fun formatSyncStateMessage(
        state: SyncState?
    ): String {
        return when {
            state?.progress == SyncProgressStatus.SYNCING -> {
                StringUtils.EMPTY
            }

            state?.progress == SyncProgressStatus.DOWNLOADING -> {
                resourceProvider.getString(R.string.downloading)
            }

            state?.progress == SyncProgressStatus.UPLOADING -> {
                resourceProvider.getString(R.string.uploading)
            }

            state?.status == SyncStatus.NO_CHANGES -> {
                resourceProvider.getString(R.string.file_is_up_to_date)
            }

            state?.status == SyncStatus.LOCAL_CHANGES -> {
                resourceProvider.getString(R.string.not_synchronized)
            }

            state?.status == SyncStatus.REMOTE_CHANGES -> {
                StringUtils.EMPTY
            }

            state?.status == SyncStatus.LOCAL_CHANGES_NO_NETWORK -> {
                resourceProvider.getString(R.string.not_synchronized_and_offline_mode)
            }

            state?.status == SyncStatus.NO_NETWORK -> {
                resourceProvider.getString(R.string.offline_mode)
            }

            state?.status == SyncStatus.ERROR -> {
                resourceProvider.getString(R.string.sync_error_message)
            }

            state?.status == SyncStatus.AUTH_ERROR -> {
                resourceProvider.getString(R.string.sync_auth_error_message)
            }

            state?.status == SyncStatus.FILE_NOT_FOUND -> {
                resourceProvider.getString(R.string.sync_file_not_found_message)
            }

            state?.status == SyncStatus.CONFLICT -> {
                resourceProvider.getString(R.string.sync_conflict_message)
            }

            else -> {
                StringUtils.EMPTY
            }
        }
    }
}