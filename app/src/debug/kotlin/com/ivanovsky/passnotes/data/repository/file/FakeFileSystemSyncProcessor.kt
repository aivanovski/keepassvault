package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_SYNC_STATUS
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncResolution
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FakeFileFactory.FileUid
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type.LONG_DELAY
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type.MEDIUM_DELAY
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler.Type.SHORT_DELAY
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType.LOCAL
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType.REMOTE
import com.ivanovsky.passnotes.domain.SyncStrategyResolver
import timber.log.Timber

class FakeFileSystemSyncProcessor(
    private val storage: FakeFileStorage,
    private val observerBus: ObserverBus,
    private val throttler: ThreadThrottler,
    private val fsAuthority: FSAuthority
) : FileSystemSyncProcessor {

    private val uidToSyncProgressStatusMap = mutableMapOf<String, SyncProgressStatus>()
    private val syncStrategyResolver = SyncStrategyResolver()

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

        val localFile = storage.getLocalFile(uid)
            ?: return OperationResult.error(newFileNotFoundError(uid))

        val remoteFile = storage.getRemoteFile(uid)
            ?: return OperationResult.error(newFileNotFoundError(uid))

        val conflict = SyncConflictInfo(
            localFile = localFile,
            remoteFile = remoteFile
        )

        return OperationResult.success(conflict)
    }

    override fun process(
        file: FileDescriptor,
        syncStrategy: SyncStrategy,
        resolutionStrategy: ConflictResolutionStrategy?
    ): OperationResult<FileDescriptor> {
        val localFile = storage.getLocalFile(file.uid)
        val remoteFile = storage.getRemoteFile(file.uid)

        val resolution = syncStrategyResolver.resolve(
            localModified = localFile?.modified,
            cachedRemoteModified = null,
            remoteModified = remoteFile?.modified,
            syncStrategy = if (resolutionStrategy == null) {
                SyncStrategy.LAST_MODIFICATION_WINS
            } else {
                syncStrategy
            }
        )

        Timber.d(
            "process: resolution=%s, resolutionStrategy=%s, file.uid=%s",
            resolution,
            resolutionStrategy,
            file.uid
        )

        return when {
            resolution == SyncResolution.REMOTE -> {
                downloadRemoteFile(file)
            }

            resolution == SyncResolution.LOCAL -> {
                uploadLocalFile(file)
            }

            resolution == SyncResolution.ERROR && resolutionStrategy != null -> {
                when (resolutionStrategy) {
                    ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE -> {
                        uploadLocalFile(file)
                    }

                    ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE -> {
                        downloadRemoteFile(file)
                    }
                }
            }

            else -> {
                OperationResult.error(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS))
            }
        }
    }

    private fun uploadLocalFile(file: FileDescriptor): OperationResult<FileDescriptor> {
        uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.SYNCING
        notifySyncProgressChanges(file.uid, SyncProgressStatus.SYNCING)
        throttler.delay(MEDIUM_DELAY)

        uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.UPLOADING
        notifySyncProgressChanges(file.uid, SyncProgressStatus.UPLOADING)

        throttler.delay(LONG_DELAY)

        val bytes = storage.get(file.uid, destination = LOCAL)
            ?: return OperationResult.error(newGenericIOError("File content not found"))

        storage.put(file.uid, destination = LOCAL, bytes)

        uidToSyncProgressStatusMap.remove(file.uid)
        storage.putSyncStatus(file.uid, SyncStatus.NO_CHANGES)
        notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

        return OperationResult.success(file)
    }

    private fun downloadRemoteFile(file: FileDescriptor): OperationResult<FileDescriptor> {
        uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.SYNCING
        notifySyncProgressChanges(file.uid, SyncProgressStatus.SYNCING)
        throttler.delay(MEDIUM_DELAY)

        uidToSyncProgressStatusMap[file.uid] = SyncProgressStatus.DOWNLOADING
        notifySyncProgressChanges(file.uid, SyncProgressStatus.DOWNLOADING)
        throttler.delay(LONG_DELAY)

        val bytes = storage.get(file.uid, destination = REMOTE)
            ?: return OperationResult.error(newGenericIOError("File content not found"))

        storage.put(file.uid, destination = LOCAL, bytes)

        uidToSyncProgressStatusMap.remove(file.uid)
        storage.putSyncStatus(file.uid, SyncStatus.NO_CHANGES)
        notifySyncProgressChanges(file.uid, SyncProgressStatus.IDLE)

        return OperationResult.success(file)
    }

    private fun notifySyncProgressChanges(uid: String, status: SyncProgressStatus) {
        observerBus.notifySyncProgressStatusChanged(fsAuthority, uid, status)
    }

    private fun newFileNotFoundError(pathOrUid: String): OperationError {
        return OperationError.newFileNotFoundError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_FIND_FILE,
                pathOrUid
            )
        )
    }
}