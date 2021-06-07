package com.ivanovsky.passnotes.presentation.unlock.cells.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
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
        syncStatus: SyncStatus?,
        isNextButtonVisible: Boolean,
        onFileClicked: (file: FileDescriptor) -> Unit
    ): DatabaseCellModel {
        val isStatusVisible = (syncStatus != SyncStatus.CONFLICT)

        return DatabaseCellModel(
            id = file.uid,
            name = file.name,
            path = file.formatReadablePath(resourceProvider),
            status = formatSyncStatus(syncStatus),
            isStatusVisible = isStatusVisible,
            isNextButtonVisible = isNextButtonVisible,
            onClicked = { onFileClicked.invoke(file) }
        )
    }

    private fun formatSyncStatus(syncStatus: SyncStatus?): String {
        return when (syncStatus) {
            SyncStatus.NO_CHANGES -> {
                resourceProvider.getString(R.string.file_is_up_to_date)
            }
            SyncStatus.LOCAL_CHANGES -> {
                resourceProvider.getString(R.string.unsent_changes)
            }
            SyncStatus.REMOTE_CHANGES -> {
                resourceProvider.getString(R.string.new_version_of_file_is_available)
            }
            SyncStatus.LOCAL_CHANGES_NO_NETWORK -> {
                resourceProvider.getString(R.string.unsent_changes_and_offline_mode)
            }
            SyncStatus.NO_NETWORK -> {
                resourceProvider.getString(R.string.offline_mode)
            }
            SyncStatus.CONFLICT -> EMPTY
            else -> {
                resourceProvider.getString(
                    R.string.text_with_dots,
                    resourceProvider.getString(R.string.checking)
                )
            }
        }
    }
}