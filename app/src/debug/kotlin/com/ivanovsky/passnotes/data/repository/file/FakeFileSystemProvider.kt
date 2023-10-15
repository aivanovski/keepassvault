package com.ivanovsky.passnotes.data.repository.file

import android.content.Context
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.delay.ThreadThrottler
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider
import com.ivanovsky.passnotes.extensions.map
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import timber.log.Timber

class FakeFileSystemProvider(
    private val context: Context,
    throttler: ThreadThrottler,
    observerBus: ObserverBus,
    fsAuthority: FSAuthority
) : FileSystemProvider {

    private val provider = RegularFileSystemProvider(
        context,
        FSAuthority(
            credentials = null,
            type = FSType.EXTERNAL_STORAGE,
            isBrowsable = true
        )
    )

    private val fileFactory = FakeFileFactory(fsAuthority)
    private val authenticator = FakeFileSystemAuthenticator(fsAuthority)
    private val syncProcessor = FakeFileSystemSyncProcessor(observerBus, throttler, fsAuthority)

    private val allFiles = listOf(
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

        return provider.getParent(file)
            .map { descriptor -> descriptor.substituteFsAuthority() }
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

        val realPath = getRealFilePath(file, options)
        return try {
            OperationResult.success(context.assets.open(realPath))
        } catch (exception: FileNotFoundException) {
            Timber.w(exception)
            OperationResult.error(newFileNotFoundError())
        } catch (exception: IOException) {
            Timber.w(exception)
            OperationResult.error(OperationError.newGenericIOError(exception))
        }
    }

    private fun getRealFilePath(
        file: FileDescriptor,
        options: FSOptions
    ): String {
        return when (file.uid) {
            FakeFileFactory.FileUid.CONFLICT -> {
                if (options.isCacheEnabled && options.isCacheOnly) {
                    DATABASE_FILE_NAME
                } else {
                    MODIFIED_DATABASE_FILE_NAME
                }
            }

            else -> DATABASE_FILE_NAME
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

        return provider.openFileForWrite(file, onConflictStrategy, options)
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

    companion object {
        private const val SERVER_URL = "test://server.com"
        private const val USERNAME = "user"
        private const val PASSWORD = "abc123"
        private const val DATABASE_FILE_NAME = "fake-fs-database.kdbx"
        private const val MODIFIED_DATABASE_FILE_NAME = "fake-fs-database-modified.kdbx"
    }
}