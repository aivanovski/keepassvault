package com.ivanovsky.passnotes.data.repository.file.fake

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.MergeFiles
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
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy
import com.ivanovsky.passnotes.data.repository.file.fake.FakeFileFactory.FileUid
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottler
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottler.Type.LONG_DELAY
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottler.Type.MEDIUM_DELAY
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottler.Type.SHORT_DELAY
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.BASE
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.LOCAL
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.REMOTE
import com.ivanovsky.passnotes.domain.SyncStrategyResolver
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.toOperationResult
import java.io.ByteArrayInputStream
import timber.log.Timber

class FakeFileSystemSyncProcessor(
    private val fileSystemResolver: FileSystemResolver,
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

        return storage.getSyncStatus(uid) ?: SyncStatus.FILE_NOT_FOUND
    }

    override fun getMergeFiles(uid: String): OperationResult<MergeFiles> =
        either {
            if (uid != FileUid.THREE_WAY_MERGE) {
                raise(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS, Stacktrace()))
            }

            val baseFile = storage.getBaseFile(uid).bind()
            val localFile = storage.getLocalFile(uid).bind()
            val remoteFile = storage.getRemoteFile(uid).bind()

            val baseBytes = storage.getContentOrNull(uid, BASE)
                ?: raise(newFileNotFoundError(uid))

            val localBytes = storage.getContentOrNull(uid, LOCAL)
                ?: raise(newFileNotFoundError(uid))

            val remoteBytes = storage.getContentOrNull(uid, REMOTE)
                ?: raise(newFileNotFoundError(uid))

            val baseProxyFile = createTemporalFile(
                fileSystemResolver,
                baseFile,
                baseBytes
            ).bind()

            val localProxyFile = createTemporalFile(
                fileSystemResolver,
                localFile,
                localBytes
            ).bind()

            val remoteProxyFile = createTemporalFile(
                fileSystemResolver,
                remoteFile,
                remoteBytes
            ).bind()

            MergeFiles(
                base = baseProxyFile,
                local = localProxyFile,
                remote = remoteProxyFile,
                output = remoteFile
            )
        }.toOperationResult()

    override fun getSyncConflictForFile(uid: String): OperationResult<SyncConflictInfo> {
        if (uid != FileUid.THREE_WAY_MERGE) {
            return OperationResult.error(
                newGenericError(MESSAGE_INCORRECT_SYNC_STATUS, Stacktrace())
            )
        }

        val baseFile = storage.getBaseFileOrNull(uid)
        val localFile = storage.getLocalFileOrNull(uid) ?: return OperationResult.error(
            newFileNotFoundError(uid)
        )

        val remoteFile = storage.getRemoteFileOrNull(uid) ?: return OperationResult.error(
            newFileNotFoundError(uid)
        )

        val conflict = SyncConflictInfo(
            localFile = localFile,
            remoteFile = remoteFile,
            isMergeAvailable = (baseFile != null)
        )

        return OperationResult.success(conflict)
    }

    override fun process(
        file: FileDescriptor,
        syncStrategy: SyncStrategy,
        resolutionStrategy: ConflictResolutionStrategy?
    ): OperationResult<FileDescriptor> {
        val localFile = storage.getLocalFileOrNull(file.uid)
        val remoteFile = storage.getRemoteFileOrNull(file.uid)

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
                OperationResult.error(
                    newGenericError(
                        MESSAGE_INCORRECT_SYNC_STATUS,
                        Stacktrace()
                    )
                )
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

        val bytes =
            storage.getContentOrNull(file.uid, destination = LOCAL) ?: return OperationResult.error(
                newGenericIOError(
                    "File content not found",
                    Stacktrace()
                )
            )

        storage.putContent(file.uid, destination = LOCAL, bytes)

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

        val bytes = storage.getContentOrNull(file.uid, destination = REMOTE)
            ?: return OperationResult.error(
                newGenericIOError("File content not found", Stacktrace())
            )

        storage.putContent(file.uid, destination = LOCAL, bytes)

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
            ),
            Stacktrace()
        )
    }

    private fun createTemporalFile(
        fsResolver: FileSystemResolver,
        file: FileDescriptor,
        content: ByteArray
    ): Either<OperationError, FileDescriptor> =
        either {
            val tempFile = FileUtils.createTemporalFile(fsResolver, file)
                .bind()

            val fsProvider = fsResolver.resolveProvider(tempFile.fsAuthority)

            val out = fsProvider.openFileForWrite(
                tempFile,
                OnConflictStrategy.REWRITE,
                FSOptions.NO_CACHE
            ).toEither().bind()

            InputOutputUtils.copy(
                from = ByteArrayInputStream(content),
                to = out,
                isClose = true
            ).toEither().bind()

            tempFile
        }
}