package com.ivanovsky.passnotes.presentation.selectdb.cells.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
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
                    fileUid = uid,
                    syncStatus = null
                )
            )
        }
            .toLinkedMap()
    }

    fun createCellModel(
        file: FileDescriptor,
        fileUid: UUID,
        syncStatus: SyncStatus?
    ): BaseCellModel =
        createDatabaseFileCell(
            file = file,
            fileUid = fileUid,
            syncStatus = syncStatus
        )

    private fun createDatabaseFileCell(
        file: FileDescriptor,
        fileUid: UUID,
        syncStatus: SyncStatus?
    ): BaseCellModel =
        DatabaseFileCellModel(
            id = fileUid,
            name = file.name,
            path = file.formatReadablePath(resourceProvider),
            status = formatSyncStatus(syncStatus),
            isRemoveButtonVisible = true,
            isResolveButtonVisible = (syncStatus == SyncStatus.CONFLICT)
        )

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
            SyncStatus.CONFLICT -> {
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