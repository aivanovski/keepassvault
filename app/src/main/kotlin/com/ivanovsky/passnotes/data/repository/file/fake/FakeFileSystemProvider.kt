package com.ivanovsky.passnotes.data.repository.file.fake

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.fake.delay.ThreadThrottler
import com.ivanovsky.passnotes.data.repository.file.fake.entity.StorageDestinationType
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.util.FileUtils
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

    private val authenticator = FakeFileSystemAuthenticator(fsAuthority)

    private val storage = FakeFileStorage(
        authenticator = authenticator,
        newDatabaseFactory = { byteArrayOf() },
        initialEntries = FakeFileFactory(fsAuthority).createDefaultFiles()
    )

    private val syncProcessor = FakeFileSystemSyncProcessor(
        storage = storage,
        observerBus = observerBus,
        throttler = throttler,
        fsAuthority = fsAuthority
    )

    override fun getAuthenticator(): FileSystemAuthenticator {
        return authenticator
    }

    override fun getSyncProcessor(): FileSystemSyncProcessor {
        return syncProcessor
    }

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        if (!isAuthenticated()) {
            return OperationResult.error(newAuthError())
        }

        return OperationResult.success(storage.listFiles(dir.path))
    }

    override fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
        if (!isAuthenticated()) {
            return OperationResult.error(newAuthError())
        }

        val parentPath = FileUtils.getParentPath(file.path)
            ?: return OperationResult.error(newFileNotFoundError(file.path))

        val parent = storage.getFileByPath(parentPath)
            ?: return OperationResult.error(newFileNotFoundError(parentPath))

        return OperationResult.success(parent)
    }

    override fun getRootFile(): OperationResult<FileDescriptor> {
        if (!isAuthenticated()) {
            return OperationResult.error(newAuthError())
        }

        val root = storage.getFileByPath(ROOT)
            ?: return OperationResult.error(newFileNotFoundError(ROOT))

        return OperationResult.success(root)
    }

    override fun openFileForRead(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<InputStream> {
        Timber.d(
            "openFileForRead: uid=%s, options=%s, isAuthenticated=%s",
            file.uid,
            options,
            isAuthenticated()
        )

        if (!isAuthenticated()) {
            return OperationResult.error(newAuthError())
        }

        val content = storage.get(file.uid, options)
            ?: return OperationResult.error(newFileNotFoundError(file.uid))

        return try {
            OperationResult.success(ByteArrayInputStream(content))
        } catch (exception: FileNotFoundException) {
            Timber.w(exception)
            OperationResult.error(newFileNotFoundError(file.uid))
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
        Timber.d(
            "openFileForWrite: uid=%s, options=%s, isAuthenticated=%s",
            file.uid,
            options,
            isAuthenticated()
        )

        if (!isAuthenticated()) {
            return OperationResult.error(newAuthError())
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
            return OperationResult.error(newAuthError())
        }

        val isExist = (storage.getFileByPath(file.path) != null)
        return OperationResult.success(isExist)
    }

    override fun getFile(path: String, options: FSOptions): OperationResult<FileDescriptor> {
        if (!isAuthenticated()) {
            return OperationResult.error(newAuthError())
        }

        val file = storage.getFileByPath(path)

        return if (file != null) {
            OperationResult.success(file)
        } else {
            OperationResult.error(newFileNotFoundError(path))
        }
    }

    private fun isAuthenticated(): Boolean {
        val creds = authenticator.getFsAuthority().credentials ?: return false
        return creds == FS_AUTHORITY.credentials
    }

    private fun newAuthError(): OperationError {
        return OperationError.newAuthError(Stacktrace())
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

    companion object {
        private const val ROOT = "/"

        val FS_AUTHORITY = FSAuthority(
            credentials = FSCredentials.BasicCredentials(
                url = "content://fakefs.com",
                username = "user",
                password = "abc123",
                isIgnoreSslValidation = false
            ),
            type = FSType.FAKE,
            isBrowsable = true
        )
    }
}