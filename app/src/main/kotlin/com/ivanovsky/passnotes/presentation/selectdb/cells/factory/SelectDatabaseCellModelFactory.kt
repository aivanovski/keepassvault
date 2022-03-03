package com.ivanovsky.passnotes.presentation.selectdb.cells.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadablePath
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.selectdb.cells.model.DatabaseFileCellModel
import com.ivanovsky.passnotes.util.toLinkedMap
import java.util.UUID

class SelectDatabaseCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createCellModels(
        files: List<Pair<UUID, FileDescriptor>>
    ): MutableMap<UUID, BaseCellModel> {
        return files.map { (uid, file) ->
            Pair(
                uid,
                createDatabaseFileCell(
                    file = file,
                    cellUid = uid,
                    syncState = null
                )
            )
        }
            .toLinkedMap()
    }

    fun createCellModel(
        file: FileDescriptor,
        cellUid: UUID,
        syncState: SyncState?
    ): BaseCellModel =
        createDatabaseFileCell(
            file = file,
            cellUid = cellUid,
            syncState = syncState
        )

    private fun createDatabaseFileCell(
        file: FileDescriptor,
        cellUid: UUID,
        syncState: SyncState?
    ): BaseCellModel =
        DatabaseFileCellModel(
            id = cellUid,
            name = file.name,
            path = file.formatReadablePath(resourceProvider),
            status = formatSyncStatus(syncState),
            isRemoveButtonVisible = true,
            isResolveButtonVisible = (syncState?.status == SyncStatus.CONFLICT)
        )

    private fun formatSyncStatus(state: SyncState?): String {
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
            state?.status == SyncStatus.CONFLICT -> {
                resourceProvider.getString(R.string.conflict)
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