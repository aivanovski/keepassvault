package com.ivanovsky.passnotes.data.repository.file.fake

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.COMBINED_KEY
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.COMBINED_XML_KEY
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.FILE_KEY
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.PASSWORD_KEY
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.XML_FILE_KEY
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createAutomationDatabase
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDatabaseWithAttachmentsData
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDatabaseWithCombinedKey
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDatabaseWithExpiredData
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDatabaseWithHistoryData
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDatabaseWithKeyFile
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDatabaseWithOtpData
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDemoDatabase
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDiffDatabase
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createDiffModifiedDatabase
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createKeyFileData
import com.ivanovsky.passnotes.data.repository.file.fake.FakeDatabaseContentFactory.createXmlKeyFileData
import com.ivanovsky.passnotes.data.repository.file.fake.entity.FakeStorageEntry
import com.ivanovsky.passnotes.util.FileUtils

class FakeFileFactory(
    private val fsAuthority: FSAuthority
) {

    fun createDefaultFiles(): List<FakeStorageEntry> {
        return listOf(
            // directories
            newEntry(newDirectory("/", Time.ROOT)),
            newEntry(newDirectory("/automation", Time.ROOT)),
            newEntry(newDirectory("/conflicts", Time.ROOT)),
            newEntry(newDirectory("/errors", Time.ROOT)),
            newEntry(newDirectory("/examples", Time.ROOT)),
            newEntry(newDirectory("/keys", Time.ROOT)),
            newEntry(newDirectory("/demo", Time.ROOT)),

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

            newEntry(
                localFile = newFile(Path.ATTACHMENTS, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDatabaseWithAttachmentsData() },
                remoteContentFactory = { createDatabaseWithAttachmentsData() }
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
            ),
            newEntry(
                localFile = newFile(Path.XML_KEY, Time.NO_CHANGES),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createXmlKeyFileData() },
                remoteContentFactory = { createXmlKeyFileData() }
            ),

            // demo
            newEntry(
                localFile = newFile(Path.PASSWORDS, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDemoDatabase() },
                remoteContentFactory = { createDemoDatabase() }
            ),

            newEntry(
                localFile = newFile(Path.PASSWORDS_MODIFID, Time.REMOTE),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = REMOTE_CONTENT_FACTORY,
                remoteContentFactory = REMOTE_CONTENT_FACTORY
            ),

            // diff
            newEntry(
                localFile = newFile(Path.DETAILED_DIFF, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDiffDatabase() },
                remoteContentFactory = { createDiffDatabase() }
            ),
            newEntry(
                localFile = newFile(Path.DETAILED_DIFF_MODIFIED, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createDiffModifiedDatabase() },
                remoteContentFactory = { createDiffModifiedDatabase() }
            ),

            // automation
            newEntry(
                localFile = newFile(Path.AUTOMATION_PASSWORD_UNLOCK, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createAutomationDatabase(PASSWORD_KEY) },
                remoteContentFactory = { createAutomationDatabase(PASSWORD_KEY) }
            ),
            newEntry(
                localFile = newFile(Path.AUTOMATION_KEY_FILE_UNLOCK, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createAutomationDatabase(FILE_KEY) },
                remoteContentFactory = { createAutomationDatabase(FILE_KEY) }
            ),
            newEntry(
                localFile = newFile(Path.AUTOMATION_COMBINED_UNLOCK, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createAutomationDatabase(COMBINED_KEY) },
                remoteContentFactory = { createAutomationDatabase(COMBINED_KEY) }
            ),
            newEntry(
                localFile = newFile(Path.AUTOMATION_XML_KEY_FILE_UNLOCK, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createAutomationDatabase(XML_FILE_KEY) },
                remoteContentFactory = { createAutomationDatabase(XML_FILE_KEY) }
            ),
            newEntry(
                localFile = newFile(Path.AUTOMATION_COMBINED_XML_UNLOCK, Time.LOCAL),
                syncStatus = SyncStatus.NO_CHANGES,
                localContentFactory = { createAutomationDatabase(COMBINED_XML_KEY) },
                remoteContentFactory = { createAutomationDatabase(COMBINED_XML_KEY) }
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
        const val CONFLICT = "conflict"
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
        val ATTACHMENTS = "/examples/test-attachments.kdbx"

        // conflicts
        val CONFLICT = "/conflicts/test-conflict.kdbx"

        // keys
        val KEY = "/keys/key"
        val XML_KEY = "/keys/xml-key.keyx"

        // demo
        val PASSWORDS = "/demo/passwords.kdbx"
        val PASSWORDS_MODIFID = "/demo/passwords-modified.kdbx"

        // diff
        val DETAILED_DIFF = "/examples/detailed-diff.kdbx"
        val DETAILED_DIFF_MODIFIED = "/examples/detailed-diff-modified.kdbx"

        // automation
        val AUTOMATION_PASSWORD_UNLOCK = "/automation/basic.kdbx"
        val AUTOMATION_KEY_FILE_UNLOCK = "/automation/basic-key-file.kdbx"
        val AUTOMATION_COMBINED_UNLOCK = "/automation/basic-key-file-and-password.kdbx"
        val AUTOMATION_XML_KEY_FILE_UNLOCK = "/automation/basic-xml-key-file.kdbx"
        val AUTOMATION_COMBINED_XML_UNLOCK = "/automation/basic-xml-key-file-and-password.kdbx"
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