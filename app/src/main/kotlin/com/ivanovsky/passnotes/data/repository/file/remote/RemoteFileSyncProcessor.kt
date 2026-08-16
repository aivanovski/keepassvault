package com.ivanovsky.passnotes.data.repository.file.remote

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.MergeFiles
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncResolution
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.RemoteFileInputStream
import com.ivanovsky.passnotes.domain.SyncResolutionResolver
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.extensions.toFileDescriptor
import com.ivanovsky.passnotes.util.FileUtils.copyFile
import com.ivanovsky.passnotes.util.FileUtils.createTemporalFile
import com.ivanovsky.passnotes.util.isNewerThan
import com.ivanovsky.passnotes.util.toOperationResult
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

class RemoteFileSyncProcessor(
    private val fileSystemResolver: FileSystemResolver,
    private val provider: RemoteFileSystemProvider,
    private val cache: RemoteFileCache,
    private val observerBus: ObserverBus,
    private val fsAuthority: FSAuthority
) : FileSystemSyncProcessor {

    private val syncResolver = SyncResolutionResolver()
    private val progressStatuses = ConcurrentHashMap<String, SyncProgressStatus>()
    private val statuses = ConcurrentHashMap<String, SyncStatus>()

    override fun getCachedFile(uid: String): FileDescriptor? =
        cache.getByUid(uid)?.toFileDescriptor()

    override fun getSyncProgressStatusForFile(uid: String): SyncProgressStatus =
        progressStatuses[uid] ?: SyncProgressStatus.IDLE

    override fun getSyncStatusForFile(uid: String): SyncStatus {
        val cachedStatus = statuses[uid]
        if (cachedStatus != null) {
            return cachedStatus
        }

        val cachedFile = cache.getByUid(uid) ?: return SyncStatus.NO_CHANGES

        val localFile = File(cachedFile.localPath)
        if (!localFile.exists()) {
            return SyncStatus.FILE_NOT_FOUND
        }

        val getFile = provider.getFile(cachedFile.remotePath, FSOptions.noCache())
        if (getFile.isFailed) {
            val errorType = getFile.error.type

            return if (errorType == OperationError.Type.NETWORK_IO_ERROR) {
                if (cachedFile.isLocallyModified) {
                    SyncStatus.LOCAL_CHANGES_NO_NETWORK
                } else {
                    SyncStatus.NO_NETWORK
                }
            } else if (errorType == OperationError.Type.AUTH_ERROR) {
                SyncStatus.AUTH_ERROR
            } else {
                SyncStatus.ERROR
            }
        }

        val remoteFile = getFile.getOrThrow()

        return if (cachedFile.isLocallyModified) {
            val resolution = syncResolver.resolve(
                isLocalModified = cachedFile.isLocallyModified,
                isRemoteModified = syncResolver.isRemoteModified(
                    lastDownloadTimestamp = cachedFile.lastDownloadTimestamp,
                    remoteModifiedTimestamp = remoteFile.modified
                ),
                requestedResolution = RequestedSyncResolution.NOT_SPECIFIED
            )

            when (resolution) {
                SyncResolution.UPLOAD_LOCAL -> SyncStatus.LOCAL_CHANGES
                SyncResolution.DOWNLOAD_REMOTE -> SyncStatus.REMOTE_CHANGES
                SyncResolution.CONFLICT -> SyncStatus.CONFLICT
                SyncResolution.NO_CHANGES -> SyncStatus.NO_CHANGES
            }
        } else {
            if (remoteFile.modified.isNewerThan(cachedFile.lastRemoteModificationTimestamp)) {
                SyncStatus.REMOTE_CHANGES
            } else {
                SyncStatus.NO_CHANGES
            }
        }
    }

    override fun getRevision(uid: String): String? =
        cache.getByUid(uid)?.revision

    override fun getMergeFiles(uid: String): OperationResult<MergeFiles> =
        either {
            val cachedFile = cache.getByUid(uid) ?: raise(newCachedFileNotFoundError(uid))

            checkFileExists(path = cachedFile.localPath).bind()
            checkFileExists(path = cachedFile.localBackupPath).bind()

            val outputFile = cachedFile.toFileDescriptor()

            val baseFile = outputFile.copy(
                modified = cachedFile.lastDownloadTimestamp
            )

            val localFile = outputFile

            val remoteFile = provider.getFile(
                outputFile.path,
                FSOptions.NO_CACHE
            ).toEither().bind()

            val baseProxyFile = createTemporalFile(fileSystemResolver, baseFile).bind()
            val localProxyFile = createTemporalFile(fileSystemResolver, localFile).bind()
            val remoteProxyFile = createTemporalFile(fileSystemResolver, remoteFile).bind()

            copyFile(
                fileSystemResolver = fileSystemResolver,
                source = File(cachedFile.localBackupPath),
                destination = baseProxyFile
            ).bind()

            copyFile(
                fileSystemResolver = fileSystemResolver,
                source = File(cachedFile.localPath),
                destination = localProxyFile
            ).bind()

            copyFile(
                fileSystemResolver = fileSystemResolver,
                source = remoteFile,
                destination = remoteProxyFile
            ).bind()

            MergeFiles(
                base = baseProxyFile,
                local = localProxyFile,
                remote = remoteProxyFile,
                output = remoteFile
            )
        }.toOperationResult()

    override fun getSyncConflictForFile(uid: String): OperationResult<SyncConflictInfo> {
        val cachedFile = cache.getByUid(uid)
            ?: return OperationResult.error(
                OperationError.newCacheError(
                    OperationError.MESSAGE_FAILED_TO_FIND_CACHED_FILE,
                    Stacktrace()
                )
            )

        val getFile =
            provider.getFile(cachedFile.remotePath, FSOptions.noCache())
        if (getFile.isFailed) {
            return getFile.takeError()
        }

        val remoteFile = getFile.getOrThrow()

        if (!cachedFile.isLocallyModified) {
            return OperationResult.error(
                OperationError.newGenericError(
                    OperationError.MESSAGE_FILE_IS_NOT_MODIFIED,
                    Stacktrace()
                )
            )
        }

        val resolution = syncResolver.resolve(
            isLocalModified = cachedFile.isLocallyModified,
            isRemoteModified = syncResolver.isRemoteModified(
                lastDownloadTimestamp = cachedFile.lastDownloadTimestamp,
                remoteModifiedTimestamp = remoteFile.modified
            ),
            requestedResolution = RequestedSyncResolution.NOT_SPECIFIED
        )
        if (resolution != SyncResolution.CONFLICT) {
            return OperationResult.error(
                OperationError.newGenericError(
                    OperationError.MESSAGE_INCORRECT_SYNC_STATUS,
                    Stacktrace()
                )
            )
        }

        val backupFile = File(cachedFile.localBackupPath)
        val isThreeWayMergeAvailable = backupFile.exists()

        val info = SyncConflictInfo(
            cachedFile.toFileDescriptor(),
            getFile.getOrThrow(),
            isThreeWayMergeAvailable
        )

        return OperationResult.success(info)
    }

    override fun process(
        file: FileDescriptor,
        requestedResolution: RequestedSyncResolution
    ): OperationResult<FileDescriptor> {
        Timber.d(
            "process: file=%s, requestedResolution=%s",
            file,
            requestedResolution
        )

        updateProgressStatusForFile(file.uid, SyncProgressStatus.SYNCING)

        val cachedFile = cache.getByUid(file.uid)
        if (cachedFile == null) {
            Timber.d("Unable to process file, no cached file")

            updateProgressStatusForFile(file.uid, SyncProgressStatus.IDLE)

            return OperationResult.error(
                OperationError.newCacheError(
                    OperationError.MESSAGE_FAILED_TO_FIND_CACHED_FILE,
                    Stacktrace()
                )
            )
        }

        val localFile = cachedFile.toFileDescriptor()

        val getFile =
            provider.getFile(localFile.path, FSOptions.noCache())
        if (getFile.isFailed) {
            Timber.d("Unable to process file, failed to get file info")

            updateProgressStatusForFile(file.uid, SyncProgressStatus.IDLE)
            return getFile.takeError()
        }

        val remoteDescriptor = getFile.getOrThrow()

        val localModified = localFile.modified
        val remoteModified = remoteDescriptor.modified

        val resolution = syncResolver.resolve(
            isLocalModified = cachedFile.isLocallyModified,
            isRemoteModified = syncResolver.isRemoteModified(
                lastDownloadTimestamp = cachedFile.lastDownloadTimestamp,
                remoteModifiedTimestamp = remoteDescriptor.modified
            ),
            requestedResolution = requestedResolution
        )
        val status = convertResolutionToStatus(resolution)
        updateSyncStatusForFile(file.uid, status)

        Timber.d(
            "process: localModified=%s, remoteModified=%s, resolution=%s, remoteFile=%s",
            localModified,
            remoteModified,
            resolution,
            remoteDescriptor
        )

        val result = when (resolution) {
            SyncResolution.UPLOAD_LOCAL -> uploadLocalFile(cachedFile, localFile)
            SyncResolution.DOWNLOAD_REMOTE -> downloadFile(cachedFile, localFile, remoteDescriptor)
            SyncResolution.NO_CHANGES -> OperationResult.success(remoteDescriptor)
            SyncResolution.CONFLICT -> {
                OperationResult.error(
                    OperationError.newDbVersionConflictError(
                        OperationError.MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE,
                        Stacktrace()
                    )
                )
            }
        }

        if (result.isFailed) {
            updateProgressStatusForFile(cachedFile.uid, SyncProgressStatus.IDLE)
            removeSyncStatusForFile(cachedFile.uid)
        }

        return result
    }

    private fun uploadLocalFile(
        cachedFile: RemoteFile,
        localDescriptor: FileDescriptor
    ): OperationResult<FileDescriptor> {
        updateProgressStatusForFile(cachedFile.uid, SyncProgressStatus.UPLOADING)

        val uploadResult = provider.uploadFromCache(localDescriptor)
        if (uploadResult.isFailed) {
            Timber.d("Failed to upload file, error=%s", uploadResult.error)
            return uploadResult.takeError()
        }

        val updatedCachedFile = cache.getByUid(cachedFile.uid)
        if (updatedCachedFile == null) {
            Timber.d("Failed to find file in cache, uid=%s", cachedFile.uid)
            return OperationResult.error(
                OperationError.newCacheError(
                    OperationError.MESSAGE_FAILED_TO_FIND_CACHED_FILE,
                    Stacktrace()
                )
            )
        }

        val localFile = uploadResult.getOrThrow().first
        val metadata = uploadResult.getOrThrow().second

        val localBackupPath = localFile.path + "_backup"
        val copyResult = copyFile(
            source = File(localFile.path),
            destination = File(localBackupPath)
        ).toOperationResult()
        if (copyResult.isFailed) {
            return copyResult.mapError()
        }

        updatedCachedFile.uid = metadata.uid
        updatedCachedFile.localPath = localFile.path
        updatedCachedFile.localBackupPath = localBackupPath
        updatedCachedFile.remotePath = metadata.path
        updatedCachedFile.revision = metadata.revision
        updatedCachedFile.lastModificationTimestamp = metadata.serverModified.time
        updatedCachedFile.lastRemoteModificationTimestamp = metadata.serverModified.time
        updatedCachedFile.lastDownloadTimestamp = System.currentTimeMillis()
        updatedCachedFile.isUploaded = true
        updatedCachedFile.isLocallyModified = false

        cache.update(updatedCachedFile)

        updateProgressStatusForFile(cachedFile.uid, SyncProgressStatus.IDLE)
        removeSyncStatusForFile(cachedFile.uid)

        return OperationResult.success(updatedCachedFile.toFileDescriptor())
    }

    private fun downloadFile(
        cachedFile: RemoteFile,
        localDescriptor: FileDescriptor,
        remoteDescriptor: FileDescriptor
    ): OperationResult<FileDescriptor> {
        Timber.d("downloadFile: file=%s", localDescriptor)

        updateProgressStatusForFile(cachedFile.uid, SyncProgressStatus.DOWNLOADING)

        val inResult = provider.openFileForRead(
            localDescriptor,
            OnConflictStrategy.REWRITE,
            FSOptions.DEFAULT
        )
        if (inResult.isFailed) {
            Timber.d("Failed to download, error=%s", inResult.error)
            return inResult.takeError()
        }

        if (inResult.getOrThrow() !is RemoteFileInputStream) {
            Timber.d("Failed to open file")
            return OperationResult.error(
                OperationError.newGenericIOError(
                    OperationError.MESSAGE_FAILED_TO_FIND_FILE,
                    Stacktrace()
                )
            )
        }

        val input = inResult.getOrThrow() as RemoteFileInputStream
        try {
            input.close()
        } catch (e: IOException) {
            Timber.d(e)
            return OperationResult.error(
                OperationError.newFileAccessError(
                    OperationError.MESSAGE_FAILED_TO_ACCESS_TO_FILE,
                    Stacktrace()
                )
            )
        }

        val updatedCachedFile = cache.getByUid(cachedFile.uid)
        if (updatedCachedFile == null) {
            Timber.d("Failed to find file in cache, uid=%s", cachedFile.uid)
            return OperationResult.error(
                OperationError.newCacheError(
                    OperationError.MESSAGE_FAILED_TO_FIND_CACHED_FILE,
                    Stacktrace()
                )
            )
        }

        val metadataResult =
            provider.getFileMetadata(remoteDescriptor)
        if (metadataResult.isFailed) {
            Timber.d("Failed to get metadata, error=%s", metadataResult.error)
            return metadataResult.takeError()
        }

        val metadata = metadataResult.getOrThrow()

        val localBackupPath = input.path + "_backup"
        val copyResult = copyFile(
            File(input.path),
            File(localBackupPath)
        ).toOperationResult()
        if (copyResult.isFailed) {
            Timber.d("Failed to create local")
            return copyResult.mapError()
        }

        updatedCachedFile.uid = metadata.uid
        updatedCachedFile.localPath = input.path
        updatedCachedFile.localBackupPath = localBackupPath
        updatedCachedFile.remotePath = metadata.path
        updatedCachedFile.revision = metadata.revision
        updatedCachedFile.lastModificationTimestamp = metadata.serverModified.time
        updatedCachedFile.lastRemoteModificationTimestamp = metadata.serverModified.time
        updatedCachedFile.lastDownloadTimestamp = System.currentTimeMillis()
        updatedCachedFile.isUploaded = true
        updatedCachedFile.isLocallyModified = false

        cache.update(updatedCachedFile)

        updateProgressStatusForFile(cachedFile.uid, SyncProgressStatus.IDLE)
        removeSyncStatusForFile(cachedFile.uid)

        return OperationResult.success(updatedCachedFile.toFileDescriptor())
    }

    private fun updateProgressStatusForFile(uid: String, status: SyncProgressStatus) {
        Timber.d("updateStatusForFile: status=%s, uid=%s", status, uid)
        val oldStatus = progressStatuses.getOrDefault(uid, SyncProgressStatus.IDLE)

        if (status != oldStatus) {
            observerBus.notifySyncProgressStatusChanged(fsAuthority, uid, status)

            if (status != SyncProgressStatus.IDLE) {
                progressStatuses[uid] = status
            } else {
                progressStatuses.remove(uid)
            }
        }
    }

    private fun updateSyncStatusForFile(uid: String, status: SyncStatus) {
        statuses[uid] = status
    }

    private fun removeSyncStatusForFile(uid: String) {
        statuses.remove(uid)
    }

    private fun convertResolutionToStatus(resolution: SyncResolution): SyncStatus {
        return when (resolution) {
            SyncResolution.UPLOAD_LOCAL -> SyncStatus.LOCAL_CHANGES
            SyncResolution.DOWNLOAD_REMOTE -> SyncStatus.REMOTE_CHANGES
            SyncResolution.CONFLICT -> SyncStatus.CONFLICT
            SyncResolution.NO_CHANGES -> SyncStatus.NO_CHANGES
        }
    }

    private fun newCachedFileNotFoundError(uid: String): OperationError =
        OperationError.newCacheError(
            OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_CACHED_FILE.format(uid),
            Stacktrace()
        )

    private fun checkFileExists(path: String?): Either<OperationError, Unit> =
        either {
            if (path == null || !File(path).exists()) {
                raise(newFileNotFoundError(path ?: ""))
            }
        }

    private fun newFileNotFoundError(pathOrUid: String): OperationError =
        OperationError.newFileNotFoundError(
            OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE.format(pathOrUid),
            Stacktrace()
        )
}