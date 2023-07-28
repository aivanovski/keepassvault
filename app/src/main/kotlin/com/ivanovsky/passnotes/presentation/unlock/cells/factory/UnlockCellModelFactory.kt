package com.ivanovsky.passnotes.presentation.unlock.cells.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadablePath
import com.ivanovsky.passnotes.extensions.getFileDescriptor
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseFileCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class UnlockCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createFileModels(
        files: List<UsedFile>,
        selectedFile: UsedFile?,
        usedFileIdToSyncStateMap: Map<Int?, SyncState?>
    ): List<BaseCellModel> {
        return files.map { file ->
            createFileModel(file, selectedFile, usedFileIdToSyncStateMap)
        }
    }

    private fun createFileModel(
        file: UsedFile,
        selectedFile: UsedFile?,
        usedFileIdToSyncStateMap: Map<Int?, SyncState?>
    ): BaseCellModel {
        val isSelected = (selectedFile != null && file.id == selectedFile.id)

        val descriptor = file.getFileDescriptor()
        val syncState = usedFileIdToSyncStateMap[file.id]
        val isCheckingStatus = (syncState == null && usedFileIdToSyncStateMap.containsKey(file.id))

        return DatabaseFileCellModel(
            id = file.id ?: -1,
            filename = file.fileName,
            path = descriptor.formatReadablePath(resourceProvider),
            status = when {
                isCheckingStatus -> {
                    resourceProvider.getString(
                        R.string.text_with_dots,
                        resourceProvider.getString(R.string.checking_status)
                    )
                }
                syncState != null -> {
                    formatSyncState(syncState)
                }
                else -> EMPTY
            },
            statusColor = getSyncStatusColor(syncState),
            isStatusVisible = true,
            isSelected = isSelected
        )
    }

    private fun getSyncStatusColor(state: SyncState?): Int {
        return when (state?.status) {
            SyncStatus.CONFLICT,
            SyncStatus.ERROR,
            SyncStatus.AUTH_ERROR,
            SyncStatus.FILE_NOT_FOUND -> {
                resourceProvider.getAttributeColor(R.attr.kpErrorTextColor)
            }
            else -> {
                resourceProvider.getAttributeColor(R.attr.kpSecondaryTextColor)
            }
        }
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
            state?.status == SyncStatus.FILE_NOT_FOUND -> {
                resourceProvider.getString(R.string.not_found)
            }
            state?.status == SyncStatus.CONFLICT -> {
                resourceProvider.getString(R.string.conflict)
            }
            else -> {
                EMPTY
            }
        }
    }
}