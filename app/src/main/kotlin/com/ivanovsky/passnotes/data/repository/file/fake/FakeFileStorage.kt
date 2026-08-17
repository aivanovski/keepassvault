package com.ivanovsky.passnotes.data.repository.file.fake

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.fake.entity.FakeStorageEntry
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.BASE
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.LOCAL
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.REMOTE
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.util.FileUtils.getParentPath
import timber.log.Timber

class FakeFileStorage(
    private val authenticator: FileSystemAuthenticator,
    private val newDatabaseFactory: DatabaseContentFactory,
    initialEntries: List<FakeStorageEntry>
) {

    private val cache = initialEntries
        .map { entry -> entry.toCacheEntry() }
        .associateBy { entry -> entry.localFile.uid }
        .toMutableMap()

    fun getLocalFile(
        uid: String
    ): Either<OperationError, FileDescriptor> =
        either {
            getLocalFileOrNull(uid) ?: raise(newFileNotFoundError(uid))
        }

    fun getRemoteFile(
        uid: String
    ): Either<OperationError, FileDescriptor> =
        either {
            getRemoteFileOrNull(uid) ?: raise(newFileNotFoundError(uid))
        }

    fun getBaseFile(
        uid: String
    ): Either<OperationError, FileDescriptor> =
        either {
            getBaseFileOrNull(uid) ?: raise(newFileNotFoundError(uid))
        }

    fun getLocalFileOrNull(uid: String): FileDescriptor? {
        return getCacheEntryOrNull(uid)?.localFile
            ?.substituteFsAuthority()
    }

    fun getRemoteFileOrNull(uid: String): FileDescriptor? {
        return getCacheEntryOrNull(uid)?.remoteFile
            ?.substituteFsAuthority()
    }

    fun getBaseFileOrNull(uid: String): FileDescriptor? {
        return getCacheEntryOrNull(uid)?.baseFile
            ?.substituteFsAuthority()
    }

    fun getSyncStatus(uid: String): SyncStatus? {
        return getCacheEntryOrNull(uid)?.syncStatus
    }

    fun putSyncStatus(uid: String, status: SyncStatus) {
        getCacheEntry(uid).syncStatus = status
    }

    fun putFile(
        file: FileDescriptor,
        content: ByteArray
    ) {
        val contentFactory = { content }

        val entry = CacheEntry(
            baseFile = file,
            localFile = file,
            remoteFile = file,
            syncStatus = SyncStatus.NO_CHANGES,
            baseContentFactory = contentFactory,
            localContentFactory = contentFactory,
            remoteContentFactory = contentFactory,
            baseContent = content,
            localContent = content,
            remoteContent = content
        )

        cache[entry.localFile.uid] = entry
    }

    fun putContent(
        uid: String,
        destination: StorageDestinationType,
        content: ByteArray
    ) {
        val entry = getCacheEntry(uid)

        when (destination) {
            BASE -> entry.baseContent = content
            LOCAL -> entry.localContent = content
            REMOTE -> entry.remoteContent = content
        }
    }

    fun getContentOrNull(
        uid: String,
        destination: StorageDestinationType
    ): ByteArray? {
        generateAndStoreContentIfNeed(uid)

        return when (destination) {
            BASE -> getCacheEntryOrNull(uid)?.baseContent
            LOCAL -> getCacheEntryOrNull(uid)?.localContent
            REMOTE -> getCacheEntryOrNull(uid)?.remoteContent
        }
    }

    fun getContentOrNull(
        uid: String,
        fsOptions: FSOptions
    ): ByteArray? {
        val destination = determineDestination(fsOptions)
        Timber.d(
            "Get content: uid=%s, fsOptions=%s, destination=%s",
            uid,
            fsOptions,
            destination
        )

        return getContentOrNull(uid, destination)
    }

    fun getFileByPath(path: String): FileDescriptor? {
        return cache.values
            .firstOrNull { entry -> entry.localFile.path == path }
            ?.localFile
            ?.substituteFsAuthority()
    }

    fun listFiles(dirPath: String): List<FileDescriptor> {
        val allFiles = cache.values.map { entry -> entry.localFile }

        return allFiles
            .filter { file -> !file.isRoot && getParentPath(file.path) == dirPath }
            .map { file -> file.substituteFsAuthority() }
    }

    private fun determineDestination(fsOptions: FSOptions): StorageDestinationType {
        return if (!fsOptions.isCacheEnabled) {
            REMOTE
        } else {
            LOCAL
        }
    }

    private fun generateAndStoreContentIfNeed(uid: String) {
        val entry = getCacheEntry(uid)

        if (entry.baseContent != null &&
            entry.localContent != null &&
            entry.remoteContent != null
        ) {
            return
        }

        val baseContent = entry.baseContentFactory.create()
        val localContent = entry.localContentFactory.create()
        val remoteContent = entry.remoteContentFactory.create()

        Timber.d(
            "Generate content: uid=%s, base=%s, local.size=%s, remote.size=%s",
            uid,
            baseContent.size,
            localContent.size,
            remoteContent.size
        )

        entry.baseContent = baseContent
        entry.localContent = localContent
        entry.remoteContent = remoteContent
    }

    private fun FileDescriptor.substituteFsAuthority(): FileDescriptor {
        return copy(fsAuthority = authenticator.getFsAuthority())
    }

    private fun getCacheEntry(uid: String): CacheEntry {
        return cache[uid] ?: throwEntryNotFound(uid)
    }

    private fun getCacheEntryOrNull(uid: String): CacheEntry? {
        return cache[uid]
    }

    private fun throwEntryNotFound(uid: String): Nothing {
        throw IllegalStateException("Unable to find data for file: uid=$uid")
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

    private fun FakeStorageEntry.toCacheEntry(): CacheEntry {
        return CacheEntry(
            baseFile = baseFile,
            localFile = localFile,
            remoteFile = remoteFile,
            syncStatus = syncStatus,
            baseContentFactory = baseContentFactory ?: newDatabaseFactory,
            localContentFactory = localContentFactory ?: newDatabaseFactory,
            remoteContentFactory = remoteContentFactory ?: newDatabaseFactory,
            baseContent = null,
            localContent = null,
            remoteContent = null
        )
    }

    private class CacheEntry(
        var baseFile: FileDescriptor,
        var localFile: FileDescriptor,
        var remoteFile: FileDescriptor,
        var syncStatus: SyncStatus,
        val baseContentFactory: DatabaseContentFactory,
        val localContentFactory: DatabaseContentFactory,
        val remoteContentFactory: DatabaseContentFactory,
        var baseContent: ByteArray?,
        var localContent: ByteArray?,
        var remoteContent: ByteArray?
    )
}