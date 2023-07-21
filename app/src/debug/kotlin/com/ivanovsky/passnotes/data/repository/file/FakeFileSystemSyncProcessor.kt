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
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type.SHORT_DELAY

class FakeFileSystemSyncProcessor(
    private val observerBus: ObserverBus,
    private val throttler: ThreadThrottler,
    private val fsAuthority: FSAuthority
) : FileSystemSyncProcessor {

    private val fileFactory = FakeFileFactory(fsAuthority)

    private val statuses = mutableMapOf<String, SyncStatus>()
    private val uidToSyncProgressStatusMap = mutableMapOf<String, SyncProgressStatus>()

    override fun getCachedFile(uid: String): FileDescriptor? {
        return null
    }

    override fun getRevision(uid: String): String? {
        return null
    }

    override fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus {
        return uidToSyncProgressStatusMap[uid] ?: SyncProgressStatus.IDLE
    }

    override fun getSyncStatusForFile(uid: String): SyncStatus {
        throttler.delay(SHORT_DELAY)

        val status = statuses[uid]
        if (status != null) {
            return status
        }

        return when (uid) {
            FileUid.CONFLICT -> SyncStatus.CONFLICT
            FileUid.REMOTE_CHANGES -> SyncStatus.REMOTE_CHANGES
            FileUid.LOCAL_CHANGES -> SyncStatus.LOCAL_CHANGES
            FileUid.LOCAL_CHANGES_TIMEOUT -> SyncStatus.LOCAL_CHANGES
            FileUid.ERROR -> SyncStatus.ERROR
            FileUid.AUTH_ERROR -> SyncStatus.AUTH_ERROR
            FileUid.NOT_FOUND -> SyncStatus.FILE_NOT_FOUND
            else -> SyncStatus.NO_CHANGES
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
        return when (file.uid) {
            FileUid.LOCAL_CHANGES, FileUid.LOCAL_CHANGES_TIMEOUT -> {
                uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.SYNCING
                notifySyncProgressChanges(file.uid, SyncProgressStatus.SYNCING)
                throttler.delay(MEDIUM_DELAY)

                uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.UPLOADING
                notifySyncProgressChanges(file.uid, SyncProgressStatus.UPLOADING)
                if (file.uid == FileUid.LOCAL_CHANGES_TIMEOUT) {
                    throttler.delay(50000L)
                } else {
                    throttler.delay(LONG_DELAY)
                }

                uidToSyncProgressStatusMap.remove(file.uid)
                statuses[file.uid] = SyncStatus.NO_CHANGES
                notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

                OperationResult.success(file)
            }

            FileUid.REMOTE_CHANGES -> {
                uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.SYNCING
                notifySyncProgressChanges(file.uid, SyncProgressStatus.SYNCING)
                throttler.delay(MEDIUM_DELAY)

                uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.DOWNLOADING
                notifySyncProgressChanges(file.uid, SyncProgressStatus.DOWNLOADING)
                throttler.delay(LONG_DELAY)

                uidToSyncProgressStatusMap.remove(file.uid)
                statuses[file.uid] = SyncStatus.NO_CHANGES
                notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

                OperationResult.success(file)
            }

            FileUid.CONFLICT -> {
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
                statuses[file.uid] = SyncStatus.NO_CHANGES
                notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

                OperationResult.success(file)
            }

            else -> {
                OperationResult.error(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS))
            }
        }
    }

    private fun notifySyncProgressChanges(uid: String, status: SyncProgressStatus) {
        observerBus.notifySyncProgressStatusChanged(fsAuthority, uid, status)
    }
}