package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FakeFileFactory.FileUid
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType.LOCAL
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType.REMOTE
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import timber.log.Timber

class FakeFileStorage(
    fsAuthority: FSAuthority,
    private val defaultStatuses: Map<String, SyncStatus>
) {

    private val fileContentFactory = FakeFileContentFactory()
    private val fileFactory = FakeFileFactory(fsAuthority)
    private val localContent = ConcurrentHashMap<String, ByteArray>()
    private val remoteContent = ConcurrentHashMap<String, ByteArray>()
    private val statuses = ConcurrentHashMap<String, SyncStatus>()
        .apply {
            putAll(defaultStatuses)
        }
    private val files = CopyOnWriteArrayList<FileDescriptor>()
        .apply {
            addAll(createDefaultFiles())
        }

    fun getSyncStatus(uid: String): SyncStatus {
        return statuses[uid] ?: SyncStatus.NO_CHANGES
    }

    fun putSyncStatus(uid: String, status: SyncStatus) {
        statuses[uid] = status
    }

    fun put(
        uid: String,
        destination: StorageDestinationType,
        content: ByteArray
    ) {
        when (destination) {
            LOCAL -> localContent[uid] = content
            REMOTE -> remoteContent[uid] = content
        }
    }

    fun get(
        uid: String,
        destination: StorageDestinationType
    ): ByteArray? {
        generateAndStoreContentIfNeed(uid)

        return when (destination) {
            LOCAL -> localContent[uid]
            REMOTE -> remoteContent[uid]
        }
    }

    fun get(
        uid: String,
        fsOptions: FSOptions
    ): ByteArray? {
        val destination = determineDestination(uid, fsOptions)
        Timber.d("Get content: uid=$uid, fsOptions=$fsOptions, destination=$destination")

        generateAndStoreContentIfNeed(uid)

        val contentMap = when (destination) {
            LOCAL -> localContent
            REMOTE -> remoteContent
        }

        return contentMap[uid]
    }

    fun getFileByUid(uid: String): FileDescriptor? {
        return files.firstOrNull { file -> file.uid == uid }
    }

    fun getFileByPath(path: String): FileDescriptor? {
        return files.firstOrNull { file -> file.path == path }
    }

    fun getFiles(): List<FileDescriptor> {
        return files
    }

    private fun determineDestination(
        uid: String,
        fsOptions: FSOptions
    ): StorageDestinationType {
        return if (!fsOptions.isCacheEnabled) {
            REMOTE
        } else {
            LOCAL
        }
    }

    private fun generateAndStoreContentIfNeed(
        uid: String
    ) {
        if (localContent.containsKey(uid) && remoteContent.containsKey(uid)) {
            return
        }

        when (uid) {
            FileUid.CONFLICT -> {
                val local = fileContentFactory.createDefaultLocalDatabase()
                val remote = fileContentFactory.createDefaultRemoteDatabase()

                Timber.d(
                    "Generate content: uid=%s, local.size=%s, remote=%s",
                    uid,
                    local.size,
                    remote.size
                )

                localContent[uid] = local
                remoteContent[uid] = remote
            }

            else -> {
                val content = when (uid) {
                    FileUid.DEMO -> fileContentFactory.createDefaultLocalDatabase()
                    FileUid.DEMO_MODIFIED -> fileContentFactory.createDefaultRemoteDatabase()
                    FileUid.OTP -> fileContentFactory.createDatabaseWithOtpData()
                    else -> fileContentFactory.createDefaultLocalDatabase()
                }

                Timber.d("Generate content: uid=$uid, size=${content.size}")

                localContent[uid] = content
                remoteContent[uid] = content

                createFileIfNeed(uid)
            }
        }
    }

    private fun createFileIfNeed(uid: String) {
        val isExist = files.any { file -> file.uid == uid }

        Timber.d(
            "createFileIfNeed: isExist=%s, uid=%s",
            isExist,
            uid
        )

        if (isExist) {
            return
        }

        val file = fileFactory.createNewFromUid(uid)
        files.add(file)
    }

    private fun createDefaultFiles(): List<FileDescriptor> {
        return listOf(
            fileFactory.createNoChangesFile(),
            fileFactory.createRemoteChangesFile(),
            fileFactory.createLocalChangesFile(),
            fileFactory.createLocalChangesTimeoutFile(),
            fileFactory.createConflictLocalFile(),
            fileFactory.createAuthErrorFile(),
            fileFactory.createNotFoundFile(),
            fileFactory.createErrorFile(),
            fileFactory.createAutoTestsFile(),
            fileFactory.createDemoFile(),
            fileFactory.createDemoModifiedFile(),
            fileFactory.createOtpFile()
        )
    }
}