package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.file.FakeFileFactory.FileUid
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler
import com.ivanovsky.passnotes.data.repository.file.entity.StorageDestinationType
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import timber.log.Timber

class FakeFileSystemProvider(
    throttler: ThreadThrottler,
    observerBus: ObserverBus,
    fsAuthority: FSAuthority
) : FileSystemProvider {

    private val storage = FakeFileStorage(
        defaultStatuses = mapOf(
            FileUid.CONFLICT to SyncStatus.CONFLICT,
            FileUid.REMOTE_CHANGES to SyncStatus.REMOTE_CHANGES,
            FileUid.LOCAL_CHANGES to SyncStatus.LOCAL_CHANGES,
            FileUid.LOCAL_CHANGES_TIMEOUT to SyncStatus.LOCAL_CHANGES,
            FileUid.ERROR to SyncStatus.ERROR,
            FileUid.AUTH_ERROR to SyncStatus.AUTH_ERROR,
            FileUid.NOT_FOUND to SyncStatus.FILE_NOT_FOUND
        )
    )

    private val authenticator = FakeFileSystemAuthenticator(fsAuthority)
    private val syncProcessor = FakeFileSystemSyncProcessor(
        storage = storage,
        observerBus = observerBus,
        throttler = throttler,
        fsAuthority = fsAuthority
    )

    private val fileFactory = FakeFileFactory(authenticator.getFsAuthority())
    private val allFiles = createFileDescriptors()

    override fun getAuthenticator(): FileSystemAuthenticator {
        return authenticator
    }

    override fun getSyncProcessor(): FileSystemSyncProcessor {
        return syncProcessor
    }

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        if (!dir.isRoot) {
            return OperationResult.error(newFileAccessError(MESSAGE_FAILED_TO_ACCESS_TO_FILE))
        }

        val files = allFiles.map { file -> file.substituteFsAuthority() }

        return OperationResult.success(files)
    }

    override fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        return rootFile
    }

    override fun getRootFile(): OperationResult<FileDescriptor> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        val root = fileFactory.createRootFile().substituteFsAuthority()
        return OperationResult.success(root)
    }

    override fun openFileForRead(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<InputStream> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        val content = storage.get(file.uid, options)
            ?: return OperationResult.error(newFileNotFoundError())

        return try {
            OperationResult.success(ByteArrayInputStream(content))
        } catch (exception: FileNotFoundException) {
            Timber.w(exception)
            OperationResult.error(newFileNotFoundError())
        } catch (exception: IOException) {
            Timber.w(exception)
            OperationResult.error(OperationError.newGenericIOError(exception))
        }
    }

    override fun openFileForWrite(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<OutputStream> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        val stream = FakeFileOutputStream(
            onFinished = { bytes ->
                storage.put(file.uid, StorageDestinationType.LOCAL, bytes)
            }
        )

        return OperationResult.success(stream)
    }

    override fun exists(file: FileDescriptor): OperationResult<Boolean> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        val isExist = allFiles.any { it.uid == file.uid }
        return OperationResult.success(isExist)
    }

    override fun getFile(path: String, options: FSOptions): OperationResult<FileDescriptor> {
        if (!isAuthenticated()) {
            return newAuthError()
        }

        val file = allFiles.find { it.path == path }
            ?.substituteFsAuthority()

        return if (file != null) {
            OperationResult.success(file)
        } else {
            OperationResult.error(newFileNotFoundError())
        }
    }

    private fun FileDescriptor.substituteFsAuthority(): FileDescriptor {
        return copy(fsAuthority = authenticator.getFsAuthority())
    }

    private fun isAuthenticated(): Boolean {
        val creds = authenticator.getFsAuthority().credentials ?: return false
        return creds is FSCredentials.BasicCredentials &&
            creds.url == SERVER_URL &&
            creds.username == USERNAME &&
            creds.password == PASSWORD
    }

    private fun <T> newAuthError(): OperationResult<T> {
        return OperationResult.error(OperationError.newAuthError())
    }

    private fun createFileDescriptors(): List<FileDescriptor> {
        return listOf(
            fileFactory.createNoChangesFile(),
            fileFactory.createRemoteChangesFile(),
            fileFactory.createLocalChangesFile(),
            fileFactory.createLocalChangesTimeoutFile(),
            fileFactory.createConflictLocalFile(),
            fileFactory.createAuthErrorFile(),
            fileFactory.createNotFoundFile(),
            fileFactory.createErrorFile(),
            fileFactory.createAutoTestsFile()
        )
    }

    companion object {
        private const val SERVER_URL = "test://server.com"
        private const val USERNAME = "user"
        private const val PASSWORD = "abc123"
    }
}