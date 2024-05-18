package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FakeDatabaseContentFactory.createDatabaseWithCombinedKey
import com.ivanovsky.passnotes.data.repository.file.FakeDatabaseContentFactory.createDatabaseWithExpiredData
import com.ivanovsky.passnotes.data.repository.file.FakeDatabaseContentFactory.createDatabaseWithHistoryData
import com.ivanovsky.passnotes.data.repository.file.FakeDatabaseContentFactory.createDatabaseWithKeyFile
import com.ivanovsky.passnotes.data.repository.file.FakeDatabaseContentFactory.createDatabaseWithOtpData
import com.ivanovsky.passnotes.data.repository.file.FakeDatabaseContentFactory.createKeyFileData
import com.ivanovsky.passnotes.data.repository.file.entity.FakeStorageEntry
import com.ivanovsky.passnotes.util.FileUtils

class FakeFileFactory(
    private val fsAuthority: FSAuthority
) {

    fun createDefaultFiles(): List<FakeStorageEntry> {
        return listOf(
            // directories
            newEntry(newDirectory("/", Time.ROOT)),
            newEntry(newDirectory("/conflicts", Time.ROOT)),
            newEntry(newDirectory("/errors", Time.ROOT)),
            newEntry(newDirectory("/examples", Time.ROOT)),
            newEntry(newDirectory("/keys", Time.ROOT)),

            newEntry(
                localFile = newFile(Path.NO_CHANGES, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = LOCAL_CONTENT_FACTORY
            ),

            newEntry(
                localFile = newFile(Path.REMOTE_CHANGES, Time.LOCAL),
                remoteFile = newFile(Path.REMOTE_CHANGES, Time.REMOTE),
                syncStatus = SyncStatus.REMOTE_CHANGES,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = REMOTE_CONTENT_FACTORY
            ),

            newEntry(
                localFile = newFile(Path.LOCAL_CHANGES, Time.REMOTE),
                remoteFile = newFile(Path.LOCAL_CHANGES, Time.LOCAL),
                syncStatus = SyncStatus.LOCAL_CHANGES,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = REMOTE_CONTENT_FACTORY
            ),

            // Files with errors
            newEntry(
                localFile = newFile(Path.NOT_FOUND, Time.NO_CHANGES),
                syncStatus = SyncStatus.FILE_NOT_FOUND,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = LOCAL_CONTENT_FACTORY
            ),

            newEntry(
                localFile = newFile(Path.AUTH_ERROR, Time.NO_CHANGES),
                syncStatus = SyncStatus.AUTH_ERROR,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = LOCAL_CONTENT_FACTORY
            ),

            newEntry(
                localFile = newFile(Path.ERROR, Time.NO_CHANGES),
                syncStatus = SyncStatus.ERROR,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = LOCAL_CONTENT_FACTORY
            ),

            // examples
            newEntry(
                localFile = newFile(Path.DEMO, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = LOCAL_CONTENT_FACTORY
            ),

            newEntry(
                localFile = newFile(Path.OTP, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDatabaseWithOtpData() },
                remoteContentFactory = { createDatabaseWithOtpData() }
            ),

            newEntry(
                localFile = newFile(Path.KEY_UNLOCK, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDatabaseWithKeyFile() },
                remoteContentFactory = { createDatabaseWithKeyFile() }
            ),

            newEntry(
                localFile = newFile(Path.KEY_PASSWORD_UNLOCK, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDatabaseWithCombinedKey() },
                remoteContentFactory = { createDatabaseWithCombinedKey() }
            ),

            newEntry(
                localFile = newFile(Path.EXPIRATIONS, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDatabaseWithExpiredData() },
                remoteContentFactory = { createDatabaseWithExpiredData() }
            ),

            newEntry(
                localFile = newFile(Path.HISTORY, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDatabaseWithHistoryData() },
                remoteContentFactory = { createDatabaseWithHistoryData() }
            ),

            // conflicts
            newEntry(
                localFile = newFile(Path.CONFLICT, Time.REMOTE),
                remoteFile = newFile(Path.CONFLICT, Time.LOCAL),
                syncStatus = SyncStatus.CONFLICT,
                localContentFactory = LOCAL_CONTENT_FACTORY,
                remoteContentFactory = REMOTE_CONTENT_FACTORY
            ),

            // keys
            newEntry(
                localFile = newFile(Path.KEY, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createKeyFileData() },
                remoteContentFactory = { createKeyFileData() }
            )
        )
    }

    private fun newEntry(
        localFile: FileDescriptor,
        remoteFile: FileDescriptor? = null,
        syncStatus: SyncStatus = SyncStatus.NO_CHANGES,
        localContentFactory: DatabaseContentFactory? = null,
        remoteContentFactory: DatabaseContentFactory? = null
    ): FakeStorageEntry {
        return FakeStorageEntry(
            localFile = localFile,
            remoteFile = remoteFile ?: localFile,
            syncStatus = syncStatus,
            localContentFactory = localContentFactory,
            remoteContentFactory = remoteContentFactory
        )
    }

    private fun newDirectory(
        path: String,
        modified: Long
    ): FileDescriptor {
        return FileDescriptor(
            fsAuthority = fsAuthority,
            path = path,
            uid = path,
            name = FileUtils.getFileNameFromPath(path),
            isDirectory = true,
            isRoot = (path == "/"),
            modified = modified
        )
    }

    private fun newFile(
        path: String,
        modified: Long
    ): FileDescriptor {
        val name = FileUtils.getFileNameFromPath(path)
        val nameWithoutExtension = FileUtils.getFileNameWithoutExtensionFromPath(name)
            ?: throw IllegalStateException()

        val uid = if (nameWithoutExtension.startsWith("test-")) {
            nameWithoutExtension.removePrefix("test-")
        } else {
            nameWithoutExtension
        }

        return FileDescriptor(
            fsAuthority = fsAuthority,
            path = path,
            uid = uid,
            name = name,
            isDirectory = false,
            isRoot = false,
            modified = modified
        )
    }

    object FileUid {
        const val NO_CHANGES = "no-changes"
        const val ROOT = "/"
        const val REMOTE_CHANGES = "remote-changes"
        const val LOCAL_CHANGES = "local-changes"
        const val LOCAL_CHANGES_TIMEOUT = "local-changes-timeout"
        const val CONFLICT = "conflict"
        const val AUTH_ERROR = "auth-error"
        const val NOT_FOUND = "not-found"
        const val ERROR = "error"
        const val AUTO_TESTS = "auto-tests"
        const val DEMO = "demo"
        const val DEMO_MODIFIED = "demo-modified"
        const val OTP = "otp"
    }

    object Path {
        val NO_CHANGES = "/test-no-changes.kdbx"
        val REMOTE_CHANGES = "/test-remote-changes.kdbx"
        val LOCAL_CHANGES = "/test-local-changes.kdbx"

        // files with errors
        val NOT_FOUND = "/errors/test-not-found.kdbx"
        val AUTH_ERROR = "/errors/test-auth-error.kdbx"
        val ERROR = "/errors/test-error.kdbx"

        // examples
        val DEMO = "/examples/demo.kdbx"
        val OTP = "/examples/test-otp.kdbx"
        val KEY_UNLOCK = "/examples/key-unlock.kdbx"
        val KEY_PASSWORD_UNLOCK = "/examples/key-and-password-unlock.kdbx"
        val EXPIRATIONS = "/examples/test-expirations.kdbx"
        val HISTORY = "/examples/test-history.kdbx"

        // conflicts
        val CONFLICT = "/conflicts/test-conflict.kdbx"

        // keys
        val KEY = "/keys/key"
    }

    private object Time {
        val ROOT = parseDate("2020-01-01")
        val NO_CHANGES = parseDate("2020-02-01")
        val LOCAL = parseDate("2020-03-01")
        val REMOTE = parseDate("2020-03-02")
    }

    companion object {

        private val LOCAL_CONTENT_FACTORY = DatabaseContentFactory {
            FakeDatabaseContentFactory.createDefaultLocalDatabase()
        }

        private val REMOTE_CONTENT_FACTORY = DatabaseContentFactory {
            FakeDatabaseContentFactory.createDefaultRemoteDatabase()
        }
    }
}