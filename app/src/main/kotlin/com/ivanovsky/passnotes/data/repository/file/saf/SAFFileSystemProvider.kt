package com.ivanovsky.passnotes.data.repository.file.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_USE_CASE
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

class SAFFileSystemProvider(
    private val context: Context
) : FileSystemProvider {

    private val authenticator = SAFFileSystemAuthenticator()
    private val syncProcessor = SAFFileSystemSyncProcessor()

    override fun getAuthenticator() = authenticator

    override fun getSyncProcessor() = syncProcessor

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        return if (dir.isRoot) {
            OperationResult.success(emptyList())
        } else {
            OperationResult.error(newGenericIOError(MESSAGE_INCORRECT_USE_CASE))
        }
    }

    override fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
        return OperationResult.error(newGenericIOError(MESSAGE_INCORRECT_USE_CASE))
    }

    override fun getRootFile(): OperationResult<FileDescriptor> {
        return OperationResult.success(ROOT_FILE)
    }

    override fun openFileForRead(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<InputStream> {
        val uri = file.getUri()

        val permissionResult = takePermission(uri)
        if (permissionResult.isFailed) {
            return permissionResult.takeError()
        }

        return try {
            val stream = context.contentResolver.openInputStream(uri)
            OperationResult.success(stream)
        } catch (e: FileNotFoundException) {
            Timber.d(e)
            OperationResult.error(failedToFindFile(uri))
        } catch (e: SecurityException) {
            Timber.d(e)
            OperationResult.error(failedToGetAccessTo(uri))
        } catch (e: Exception) {
            Timber.d(e)
            OperationResult.error(unknownError(e))
        }
    }

    override fun openFileForWrite(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<OutputStream> {
        val uri = file.getUri()

        val permissionResult = takePermission(uri)
        if (permissionResult.isFailed) {
            return permissionResult.takeError()
        }

        val stream = try {
            context.contentResolver.openOutputStream(uri)
        } catch (e: FileNotFoundException) {
            Timber.d(e)
            return OperationResult.error(failedToFindFile(uri))
        } catch (e: SecurityException) {
            Timber.d(e)
            return OperationResult.error(failedToGetAccessTo(uri))
        } catch (e: Exception) {
            Timber.d(e)
            return OperationResult.error(unknownError(e))
        } ?: return OperationResult.error(failedToGetAccessTo(uri))

        return OperationResult.success(SAFOutputStream(stream))
    }

    override fun exists(file: FileDescriptor): OperationResult<Boolean> {
        val uri = file.getUri()

        val cursor = try {
            context.contentResolver.query(uri, null, null, null, null)
                ?: return OperationResult.error(failedToRetrieveData(uri))
        } catch (e: SecurityException) {
            Timber.d(e)
            return OperationResult.error(failedToGetAccessTo(uri))
        } catch (e: Exception) {
            Timber.d(e)
            return OperationResult.error(unknownError(e))
        }

        val fileSize = cursor.use {
            if (it.count == 0) {
                return OperationResult.error(failedToRetrieveData(uri))
            }

            if (!it.columnNames.contains(OpenableColumns.SIZE)) {
                return OperationResult.error(failedToFindColumn(OpenableColumns.SIZE))
            }

            val sizeColumnIdx = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeColumnIdx)
        }

        return OperationResult.success(fileSize > 0)
    }

    override fun getFile(path: String, options: FSOptions): OperationResult<FileDescriptor> {
        val uri = Uri.parse(path)

        val cursor = try {
            context.contentResolver.query(uri, null, null, null, null)
                ?: return OperationResult.error(failedToRetrieveData(uri))
        } catch (e: SecurityException) {
            Timber.d(e)
            return OperationResult.error(failedToGetAccessTo(uri))
        } catch (e: Exception) {
            Timber.d(e)
            return OperationResult.error(unknownError(e))
        }

        val name = cursor.use {
            if (it.count == 0) {
                return OperationResult.error(failedToRetrieveData(uri))
            }

            if (!it.columnNames.contains(OpenableColumns.DISPLAY_NAME)) {
                return OperationResult.error(failedToFindColumn(OpenableColumns.DISPLAY_NAME))
            }

            val displayNameColumnIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(displayNameColumnIdx)
        }

        return OperationResult.success(
            FileDescriptor(
                fsAuthority = FSAuthority.SAF_FS_AUTHORITY,
                path = path,
                uid = path,
                name = name,
                isDirectory = false,
                isRoot = false,
                modified = null
            )
        )
    }

    private fun takePermission(uri: Uri): OperationResult<Unit> {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            OperationResult.success(Unit)
        } catch (e: SecurityException) {
            Timber.d(e)
            OperationResult.error(failedToGetAccessTo(uri))
        } catch (e: Exception) {
            Timber.d(e)
            OperationResult.error(unknownError(e))
        }
    }

    private fun failedToFindColumn(columnName: String): OperationError {
        return newGenericIOError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_COLUMN,
                columnName
            )
        )
    }

    private fun failedToRetrieveData(uri: Uri): OperationError {
        return newGenericIOError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_RETRIEVE_DATA_BY_URI,
                uri.toString()
            )
        )
    }

    private fun failedToGetAccessTo(uri: Uri): OperationError {
        return newFileAccessError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_GET_ACCESS_RIGHT_TO_URI,
                uri.toString()
            )
        )
    }

    private fun failedToFindFile(uri: Uri): OperationError {
        return newFileNotFoundError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE,
                uri.toString()
            )
        )
    }

    private fun unknownError(error: Exception): OperationError {
        return newGenericIOError(
            OperationError.MESSAGE_UNKNOWN_ERROR,
            error
        )
    }

    private fun FileDescriptor.getUri(): Uri {
        return Uri.parse(path)
    }

    companion object {
        private val ROOT_FILE = FileDescriptor(
            fsAuthority = FSAuthority.SAF_FS_AUTHORITY,
            path = ROOT_PATH,
            uid = ROOT_PATH,
            name = ROOT_PATH,
            isDirectory = true,
            isRoot = true
        )
    }
}