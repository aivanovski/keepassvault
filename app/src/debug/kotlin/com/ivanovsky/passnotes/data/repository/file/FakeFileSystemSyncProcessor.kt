package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_SYNC_STATUS
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
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
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType

class FakeFileSystemSyncProcessor(
    private val storage: FakeFileStorage,
    private val observerBus: ObserverBus,
    private val throttler: ThreadThrottler,
    private val fsAuthority: FSAuthority
) : FileSystemSyncProcessor {

    private val fileFactory = FakeFileFactory(fsAuthority)
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

        return storage.getSyncStatus(uid)
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
        return file.let { file ->
            when (file.uid) {
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
                    storage.putSyncStatus(file.uid, SyncStatus.NO_CHANGES)
                    notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

                    return@let OperationResult.success(file)
                }

                FileUid.REMOTE_CHANGES -> {
                    uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.SYNCING
                    notifySyncProgressChanges(file.uid, SyncProgressStatus.SYNCING)
                    throttler.delay(MEDIUM_DELAY)

                    uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.DOWNLOADING
                    notifySyncProgressChanges(file.uid, SyncProgressStatus.DOWNLOADING)
                    throttler.delay(LONG_DELAY)

                    uidToSyncProgressStatusMap.remove(file.uid)
                    storage.putSyncStatus(file.uid, SyncStatus.NO_CHANGES)
                    notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

                    return@let OperationResult.success(file)
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

                            val content = storage.get(file.uid, StorageDestinationType.LOCAL)
                                ?: return@let OperationResult.error(newFileNotFoundError())

                            storage.put(file.uid, StorageDestinationType.REMOTE, content)
                        }

                        else -> {
                            uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.DOWNLOADING
                            notifySyncProgressChanges(file.uid, SyncProgressStatus.DOWNLOADING)
                            throttler.delay(LONG_DELAY)

                            val content = storage.get(file.uid, StorageDestinationType.REMOTE)
                                ?: return@let OperationResult.error(newFileNotFoundError())

                            storage.put(file.uid, StorageDestinationType.LOCAL, content)
                        }
                    }

                    uidToSyncProgressStatusMap.remove(file.uid)
                    storage.putSyncStatus(file.uid, SyncStatus.NO_CHANGES)
                    notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

                    return@let OperationResult.success(file)
                }

                else -> {
                    return@let OperationResult.error(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS))
                }
            }
        }
    }

    private fun notifySyncProgressChanges(uid: String, status: SyncProgressStatus) {
        observerBus.notifySyncProgressStatusChanged(fsAuthority, uid, status)
    }
}