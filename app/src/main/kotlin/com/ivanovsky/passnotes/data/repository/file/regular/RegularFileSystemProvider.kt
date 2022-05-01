package com.ivanovsky.passnotes.data.repository.file.regular

import android.content.Context
import android.os.Build
import android.os.Environment
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_PARENT_PATH
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_ACCESS_IS_FORBIDDEN
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_IS_NOT_A_DIRECTORY
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationError.newPermissionError
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.PermissionHelper.Companion.SDCARD_PERMISSION
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock

class RegularFileSystemProvider(
    private val context: Context,
    private val fsAuthority: FSAuthority
) : FileSystemProvider {

    private val lock = ReentrantLock()
    private val syncProcessor = RegularFileSystemSyncProcessor()
    private val permissionHelper = PermissionHelper(context)
    private val authenticator = createAuthenticator(fsAuthority.type)

    override fun getAuthenticator(): FileSystemAuthenticator {
        return authenticator
    }

    override fun getSyncProcessor(): FileSystemSyncProcessor {
        return syncProcessor
    }

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        if (!dir.isDirectory) {
            return OperationResult.error(newGenericIOError(MESSAGE_FILE_IS_NOT_A_DIRECTORY))
        }

        val check = checkPermissionForPath(dir.path)
        if (check.isFailed) {
            return check.takeError()
        }

        val file = File(dir.path)
        if (!file.exists()) {
            return OperationResult.error(newFileNotFoundError())
        }

        try {
            val files = file.listFiles()
            if (files != null && files.isNotEmpty()) {
                return OperationResult.success(files.map { it.toFileDescriptor() })
            }
        } catch (e: SecurityException) {
            return OperationResult.error(
                newFileAccessError(MESSAGE_FILE_ACCESS_IS_FORBIDDEN, e)
            )
        }

        return OperationResult.success(emptyList())
    }

    override fun getParent(descriptor: FileDescriptor): OperationResult<FileDescriptor> {
        val check = checkPermissionForPath(descriptor.path)
        if (check.isFailed) {
            return check.takeError()
        }

        val file = File(descriptor.path)
        if (!file.exists()) {
            return OperationResult.error(newFileNotFoundError())
        }

        val parentFile = file.parentFile
        if (parentFile == null || !parentFile.exists()) {
            return OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_GET_PARENT_PATH))
        }

        return OperationResult.success(parentFile.toFileDescriptor())
    }

    override fun getRootFile(): OperationResult<FileDescriptor> {
        val path = when (fsAuthority.type) {
            FSType.INTERNAL_STORAGE -> {
                getInternalRoot()
            }
            FSType.EXTERNAL_STORAGE -> {
                getExternalRoots().firstOrNull()
            }
            else -> {
                throw IllegalStateException()
            }
        } ?: return OperationResult.error(newFileNotFoundError())

        val root = File(path)
        return if (root.exists()) {
            OperationResult.success(root.toFileDescriptor())
        } else {
            OperationResult.error(newFileNotFoundError())
        }
    }

    override fun openFileForRead(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<InputStream> {
        val check = checkPermissionForPath(file.path)
        if (check.isFailed) {
            return check.takeError()
        }

        lock.lock()
        return try {
            val input = BufferedInputStream(FileInputStream(file.path))
            OperationResult.success(input)
        } catch (e: FileNotFoundException) {
            Timber.d(e)
            OperationResult.error(newGenericIOError(e.message, e))
        } catch (e: Exception) {
            Timber.d(e)
            OperationResult.error(newGenericIOError(e.message, e))
        } finally {
            lock.unlock()
        }
    }

    override fun openFileForWrite(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<OutputStream> {
        if (!options.isWriteEnabled) {
            return OperationResult.error(newGenericIOError(MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED))
        }

        val check = checkPermissionForPath(file.path)
        if (check.isFailed) {
            return check.takeError()
        }

        lock.lock()
        return try {
            val out = BufferedOutputStream(FileOutputStream(file.path))
            OperationResult.success(out)
        } catch (e: FileNotFoundException) {
            Timber.d(e)
            OperationResult.error(newGenericIOError(e.message, e))
        } catch (e: Exception) {
            Timber.d(e)
            OperationResult.error(newGenericIOError(e.message, e))
        } finally {
            lock.unlock()
        }
    }

    override fun exists(file: FileDescriptor): OperationResult<Boolean> {
        val check = checkPermissionForPath(file.path)
        if (check.isFailed) {
            return check.takeError()
        }

        return OperationResult.success(file.toFile().exists())
    }

    override fun getFile(
        path: String,
        options: FSOptions
    ): OperationResult<FileDescriptor> {
        val check = checkPermissionForPath(path)
        if (check.isFailed) {
            return check.takeError()
        }

        val file = File(path)
        return if (file.exists()) {
            OperationResult.success(file.toFileDescriptor())
        } else {
            OperationResult.error(newFileNotFoundError())
        }
    }

    private fun checkPermissionForPath(path: String): OperationResult<Unit> {
        if (isPathInsideInternalStorage(path)) {
            return OperationResult.success(Unit)
        }

        if (isPathInsideExternalStorage(path)) {
            return if (Build.VERSION.SDK_INT >= 30) {
                if (Environment.isExternalStorageManager()) {
                    OperationResult.success(Unit)
                } else {
                    OperationResult.error(newPermissionError(MESSAGE_FAILED_TO_ACCESS_TO_FILE))
                }
            } else {
                if (permissionHelper.isPermissionGranted(SDCARD_PERMISSION)) {
                    OperationResult.success(Unit)
                } else {
                    OperationResult.error(newPermissionError(MESSAGE_FAILED_TO_ACCESS_TO_FILE))
                }
            }
        }

        return if (File(path).exists()) {
            OperationResult.success(Unit)
        } else {
            OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE))
        }
    }

    private fun FileDescriptor.toFile(): File {
        return File(path)
    }

    private fun File.toFileDescriptor(): FileDescriptor {
        return FileDescriptor(
            fsAuthority = fsAuthority,
            path = path,
            uid = path,
            name = name,
            isDirectory = isDirectory,
            isRoot = isRoot() || isExternalRoot() || isInternalRoot(),
            modified = lastModified()
        )
    }

    private fun File.isExternalRoot(): Boolean {
        return getExternalRoots().contains(path)
    }

    private fun File.isInternalRoot(): Boolean {
        return path == getInternalRoot()
    }

    private fun File.isRoot(): Boolean {
        return path == ROOT_PATH
    }

    @Suppress("DEPRECATION")
    private fun getExternalRoots(): List<String> {
        val roots = mutableListOf<String>()

        val external = Environment.getExternalStorageDirectory()
        if (external.exists() && !roots.contains(external.path)) {
            roots.add(external.path)
        }

        val sdcard = File("/sdcard")
        if (sdcard.exists()) {
            roots.add(sdcard.path)
        }

        return roots
    }

    private fun getInternalRoot(): String {
        return context.filesDir.path
    }

    private fun isPathInsideInternalStorage(path: String): Boolean {
        val internalRoot = getInternalRoot()
        return path == internalRoot || path.startsWith(internalRoot)
    }

    private fun isPathInsideExternalStorage(path: String): Boolean {
        return getExternalRoots()
            .any { root -> path == root || path.startsWith(root) }
    }

    private fun createAuthenticator(fsType: FSType): FileSystemAuthenticator {
        return when (fsType) {
            FSType.INTERNAL_STORAGE -> InternalStorageAuthenticator()
            FSType.EXTERNAL_STORAGE -> ExternalStorageAuthenticator(permissionHelper)
            else -> throw IllegalArgumentException()
        }
    }

    companion object {
        private const val ROOT_PATH = "/"
    }
}