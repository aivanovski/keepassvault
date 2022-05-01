package com.ivanovsky.passnotes.domain

import android.content.Context
import com.ivanovsky.passnotes.data.repository.settings.Settings
import java.io.File
import java.util.UUID

class FileHelper(
    private val context: Context,
    private val settings: Settings
) {

    val filesDir: File
        get() = context.filesDir

    val remoteFilesDir: File?
        get() {
            return if (settings.isExternalStorageCacheEnabled) {
                getExternalPrivateStorageDir(REMOTE_FILES_DIR_NAME)
            } else {
                getInternalPrivateDir(REMOTE_FILES_DIR_NAME)
            }
        }

    fun generateDestinationFileForRemoteFile(): File? {
        return generateDestinationForRemoteFile()?.let {
            File(it)
        }
    }

    fun isLocatedInInternalStorage(file: File): Boolean {
        val dataDirPath = context.filesDir?.parentFile?.path
        return dataDirPath != null && file.path.startsWith(dataDirPath)
    }

    private fun generateDestinationForRemoteFile(): String? {
        return remoteFilesDir?.let {
            it.path + "/" + UUID.randomUUID().toString()
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

    companion object {
        private const val REMOTE_FILES_DIR_NAME = "remote-files"
    }
}