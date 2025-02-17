package com.ivanovsky.passnotes.domain

import android.content.Context
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_CREATE_A_DIRECTORY
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import java.io.File
import java.util.UUID

class FileHelper(
    private val context: Context,
    private val settings: Settings
) {

    val filesDir: File?
        get() = context.filesDir

    val remoteFilesDir: File?
        get() {
            return if (settings.isExternalStorageCacheEnabled) {
                getExternalPrivateStorageDir(REMOTE_FILES_DIR_NAME)
            } else {
                getInternalPrivateDir(REMOTE_FILES_DIR_NAME)
            }
        }

    fun generateDestinationFileOrNull(): File? {
        val path = generateDestinationForFile(Location.REMOTE_FILES) ?: return null

        return File(path)
    }

    fun generateDestinationFile(): OperationResult<File> {
        val file = generateDestinationFileOrNull()

        return if (file != null) {
            OperationResult.success(file)
        } else {
            OperationResult.error(
                newGenericIOError(
                    MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE,
                    Stacktrace()
                )
            )
        }
    }

    fun generateDestinationDirectoryForSharedFile(): OperationResult<File> {
        val path = generateDestinationForFile(Location.SHARED_FILES)
            ?: return OperationResult.error(
                newGenericIOError(
                    MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE,
                    Stacktrace()
                )
            )

        val dir = File(path)

        if (!dir.mkdirs()) {
            return OperationResult.error(
                newGenericIOError(
                    MESSAGE_FAILED_TO_CREATE_A_DIRECTORY,
                    Stacktrace()
                )
            )
        }

        return OperationResult.success(dir)
    }

    fun generateDestinationForPrivateFile(name: String?): OperationResult<File> {
        val path = generateDestinationForFile(
            location = Location.PRIVATE_FILES,
            baseName = name
        ) ?: return OperationResult.error(
            newGenericIOError(
                MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE,
                Stacktrace()
            )
        )

        return OperationResult.success(File(path))
    }

    fun isLocatedInInternalStorage(file: File): Boolean {
        val dataDirPath = context.filesDir?.parentFile?.path
        return dataDirPath != null && file.path.startsWith(dataDirPath)
    }

    private fun generateDestinationForFile(
        location: Location,
        baseName: String? = null
    ): String? {
        val dir = when (location) {
            Location.REMOTE_FILES -> remoteFilesDir
            Location.SHARED_FILES -> context.cacheDir
            Location.PRIVATE_FILES -> filesDir
        } ?: return null

        return if (baseName != null) {
            val file = File(dir.path, baseName)
            if (file.exists()) {
                "${dir.path}/$baseName-${UUID.randomUUID()}"
            } else {
                file.path
            }
        } else {
            dir.path + "/" + UUID.randomUUID().toString()
        }
    }

    private fun getInternalPrivateDir(name: String): File? {
        val dir = context.getDir(name, Context.MODE_PRIVATE)
        return if (dir != null && dir.exists()) {
            dir
        } else {
            null
        }
    }

    private fun getExternalPrivateStorageDir(name: String): File? {
        val dir = context.externalCacheDir
        return if (dir != null && dir.exists()) {
            val subDir = File(dir, name)
            if (subDir.exists() || !subDir.exists() && subDir.mkdirs()) {
                subDir
            } else {
                null
            }
        } else {
            null
        }
    }

    enum class Location {
        REMOTE_FILES,
        SHARED_FILES,
        PRIVATE_FILES
    }

    companion object {
        private const val REMOTE_FILES_DIR_NAME = "remote-files"
    }
}