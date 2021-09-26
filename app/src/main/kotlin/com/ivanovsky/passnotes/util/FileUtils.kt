package com.ivanovsky.passnotes.util

object FileUtils {

    const val ROOT_PATH = "/"
    const val SEPARATOR = "/"

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
            ""
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
}