package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_SYNC_STATUS
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FakeFileFactory.FileUid
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type.LONG_DELAY
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type.MEDIUM_DELAY

class FakeFileSystemSyncProcessor(
    private val observerBus: ObserverBus,
    private val throttler: ThreadThrottler,
    private val fsAuthority: FSAuthority
) : FileSystemSyncProcessor {

    private val fileFactory = FakeFileFactory(fsAuthority)
    private val resolvedUids = mutableSetOf<String>()
    private val uidToSyncProgressStatusMap = mutableMapOf<String, SyncProgressStatus>()

    override fun getLocallyModifiedFiles(): List<FileDescriptor> {
        return emptyList()
    }

    override fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus {
        return uidToSyncProgressStatusMap[uid] ?: SyncProgressStatus.IDLE
    }

    override fun getSyncStatusForFile(uid: String): SyncStatus {
        return if (uid == FileUid.CONFLICT && !resolvedUids.contains(uid)) {
            SyncStatus.CONFLICT
        } else {
            SyncStatus.NO_CHANGES
        }
    }

    override fun getSyncConflictForFile(uid: String): OperationResult<SyncConflictInfo> {
        if (uid != FileUid.CONFLICT) {
            return OperationResult.error(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS))
        }

        val conflict = SyncConflictInfo(
            localFile = fileFactory.createConflictLocalFile(),
            remoteFile = fileFactory.createConflictRemoteFile()
        )

        return OperationResult.success(conflict)
    }

    override fun process(
        file: FileDescriptor,
        syncStrategy: SyncStrategy,
        resolutionStrategy: ConflictResolutionStrategy?
    ): OperationResult<FileDescriptor> {
        if (file.uid != FileUid.CONFLICT) {
            return OperationResult.error(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS))
        }

        uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.SYNCING
        notifySyncProgressChanges(file.uid, SyncProgressStatus.SYNCING)
        throttler.delay(MEDIUM_DELAY)

        when (resolutionStrategy) {
            ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE -> {
                uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.UPLOADING
                notifySyncProgressChanges(file.uid, SyncProgressStatus.UPLOADING)
                throttler.delay(LONG_DELAY)
            }

            else -> {
                uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.DOWNLOADING
                notifySyncProgressChanges(file.uid, SyncProgressStatus.DOWNLOADING)
                throttler.delay(LONG_DELAY)
            }
        }

        uidToSyncProgressStatusMap.remove(file.uid)
        resolvedUids.add(file.uid)
        notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

        return OperationResult.success(file)
    }

    private fun notifySyncProgressChanges(uid: String, status: SyncProgressStatus) {
        observerBus.notifySyncProgressStatusChanged(fsAuthority, uid, status)
    }
}