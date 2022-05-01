package com.ivanovsky.passnotes.presentation.unlock.cells.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadablePath
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class UnlockCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createFileCellModel(
        file: FileDescriptor,
        syncState: SyncState?,
        isNextButtonVisible: Boolean,
        onFileClicked: (file: FileDescriptor) -> Unit
    ): DatabaseCellModel {
        val fsType = file.fsAuthority.type
        val isStatusVisible = (fsType != FSType.SAF &&
            fsType != FSType.INTERNAL_STORAGE &&
            fsType != FSType.EXTERNAL_STORAGE &&
            syncState?.status != SyncStatus.CONFLICT)

        return DatabaseCellModel(
            id = file.uid,
            name = file.name,
            path = file.formatReadablePath(resourceProvider),
            status = formatSyncState(syncState),
            isStatusVisible = isStatusVisible,
            isNextButtonVisible = isNextButtonVisible,
            onClicked = { onFileClicked.invoke(file) }
        )
    }

    private fun formatSyncState(state: SyncState?): String {
        return when {
            state?.progress == SyncProgressStatus.SYNCING -> {
                resourceProvider.getString(R.string.synchronizing)
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
                resourceProvider.getString(R.string.new_version_of_file_is_available)
            }
            state?.status == SyncStatus.LOCAL_CHANGES_NO_NETWORK -> {
                resourceProvider.getString(R.string.not_synchronized_and_offline_mode)
            }
            state?.status == SyncStatus.NO_NETWORK -> {
                resourceProvider.getString(R.string.offline_mode)
            }
            state?.status == SyncStatus.ERROR -> {
                resourceProvider.getString(R.string.error_offline_mode)
            }
            state?.status == SyncStatus.AUTH_ERROR -> {
                resourceProvider.getString(R.string.auth_error_offline_mode)
            }
            state?.status == SyncStatus.CONFLICT -> {
                EMPTY
            }
            else -> {
                resourceProvider.getString(
                    R.string.text_with_dots,
                    resourceProvider.getString(R.string.checking)
                )
            }
        }
    }
}