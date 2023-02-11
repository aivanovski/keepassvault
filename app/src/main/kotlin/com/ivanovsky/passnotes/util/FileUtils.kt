package com.ivanovsky.passnotes.util

import android.webkit.MimeTypeMap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileUtils {

    const val ROOT_PATH = "/"
    const val SEPARATOR = "/"
    const val DEFAULT_DB_NAME = "database.kdbx"
    const val MIME_TYPE_TEXT = "text/plain"

    @JvmStatic
    fun removeSeparatorIfNeed(path: String): String {
        return if (path.endsWith(SEPARATOR) && path.length > 1) {
            path.substring(0, path.lastIndexOf(SEPARATOR))
        } else {
            path
        }
    }

    @JvmStatic
    fun getFileNameFromPath(filePath: String): String {
        val idx = filePath.lastIndexOf(SEPARATOR)
        return if (idx >= 0 && idx < filePath.length - 1) {
            filePath.substring(idx + 1)
        } else if (idx == 0 && filePath.length == 1) {
            filePath
        } else {
            filePath
        }
    }

    @JvmStatic
    fun getParentPath(path: String): String? {
        var parentPath: String? = null
        val idx = path.lastIndexOf(SEPARATOR)
        if (idx > 0) {
            parentPath = path.substring(0, idx)
        } else if (idx == 0) {
            parentPath = ROOT_PATH
        }
        return parentPath
    }

    @JvmStatic
    fun getFileNameWithoutExtensionFromPath(filePath: String): String? {
        val fileName = getFileNameFromPath(filePath)
        return removeFileExtensionsIfNeed(fileName)
    }

    @JvmStatic
    fun removeFileExtensionsIfNeed(fileName: String): String {
        val idx = fileName.lastIndexOf(".")
        return if (idx > 0 && idx + 1 < fileName.length) {
            fileName.substring(0, idx)
        } else {
            ""
        }
    }

    fun getMimeTypeFromName(name: String): String? {
        val extension = getExtensionFromName(name) ?: return null

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun getExtensionFromName(name: String): String? {
        val lastPointIdx = name.lastIndexOf('.')
        if (lastPointIdx < 0 || lastPointIdx == name.length - 1) {
            return null
        }

        return name.substring(lastPointIdx + 1)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(source: File, destination: File) {
        val input: InputStream = BufferedInputStream(FileInputStream(source))
        val output: OutputStream = BufferedOutputStream(FileOutputStream(destination))
        InputOutputUtils.copyOrThrow(input, output, true)
    }
}