package com.ivanovsky.passnotes.data.repository.file.fake

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.fake.entity.FakeStorageEntry
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.LOCAL
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType.REMOTE
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

    fun getLocalFile(uid: String): FileDescriptor? {
        return getCacheEntryOrNull(uid)?.localFile
            ?.substituteFsAuthority()
    }

    fun getRemoteFile(uid: String): FileDescriptor? {
        return getCacheEntryOrNull(uid)?.remoteFile
            ?.substituteFsAuthority()
    }

    fun getSyncStatus(uid: String): SyncStatus? {
        return getCacheEntryOrNull(uid)?.syncStatus
    }

    fun putSyncStatus(uid: String, status: SyncStatus) {
        getCacheEntry(uid).syncStatus = status
    }

    fun put(
        uid: String,
        destination: StorageDestinationType,
        content: ByteArray
    ) {
        val entry = getCacheEntry(uid)

        when (destination) {
            LOCAL -> entry.localContent = content
            REMOTE -> entry.remoteContent = content
        }
    }

    fun get(
        uid: String,
        destination: StorageDestinationType
    ): ByteArray? {
        generateAndStoreContentIfNeed(uid)

        return when (destination) {
            LOCAL -> getCacheEntryOrNull(uid)?.localContent
            REMOTE -> getCacheEntryOrNull(uid)?.remoteContent
        }
    }

    fun get(
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

        return get(uid, destination)
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

        if (entry.localContent != null && entry.remoteContent != null) {
            return
        }

        val localContent = entry.localContentFactory.create()
        val remoteContent = entry.remoteContentFactory.create()

        Timber.d(
            "Generate content: uid=%s, local.size=%s, remote.size=%s",
            uid,
            localContent.size,
            remoteContent.size
        )

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

    private fun FakeStorageEntry.toCacheEntry(): CacheEntry {
        return CacheEntry(
            localFile = localFile,
            remoteFile = remoteFile,
            syncStatus = syncStatus,
            localContentFactory = localContentFactory ?: newDatabaseFactory,
            remoteContentFactory = remoteContentFactory ?: newDatabaseFactory,
            localContent = null,
            remoteContent = null
        )
    }

    private class CacheEntry(
        var localFile: FileDescriptor,
        var remoteFile: FileDescriptor,
        var syncStatus: SyncStatus,
        val localContentFactory: DatabaseContentFactory,
        val remoteContentFactory: DatabaseContentFactory,
        var localContent: ByteArray?,
        var remoteContent: ByteArray?
    )
}