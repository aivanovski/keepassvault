package com.ivanovsky.passnotes.util

import android.webkit.MimeTypeMap
import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.extensions.toEither
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

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

    @JvmStatic
    fun createPath(parentPath: String, name: String): String {
        return if (parentPath.endsWith(SEPARATOR)) {
            parentPath + name
        } else {
            parentPath + SEPARATOR + name
        }
    }

    fun createTemporalFile(
        fileSystemResolver: FileSystemResolver,
        source: FileDescriptor
    ): Either<OperationError, FileDescriptor> =
        either {
            val fsProvider = fileSystemResolver.resolveProvider(FSAuthority.TEMPORAL_FS_AUTHORITY)
            val rootFile = fsProvider.rootFile.toEither().bind()

            val newPath = rootFile.path + "/" + UUID.randomUUID() + "_" + source.name

            FileDescriptor(
                fsAuthority = FSAuthority.TEMPORAL_FS_AUTHORITY,
                uid = newPath,
                path = newPath,
                name = getFileNameFromPath(newPath),
                isDirectory = false,
                isRoot = false,
                modified = source.modified
            )
        }

    fun copyFile(
        fileSystemResolver: FileSystemResolver,
        source: File,
        destination: FileDescriptor
    ): Either<OperationError, Unit> =
        either {
            val fsProvider = fileSystemResolver.resolveProvider(destination.fsAuthority)

            val output = fsProvider.openFileForWrite(
                destination,
                OnConflictStrategy.CANCEL,
                FSOptions.NO_CACHE
            ).toEither().bind()

            val input = Either
                .catch { BufferedInputStream(FileInputStream(source)) }
                .mapLeft { error -> OperationError.newGenericIOError(error) }
                .onLeft {
                    InputOutputUtils.close(output)
                }
                .bind()

            InputOutputUtils.copy(input, output, true).toEither().bind()
        }

    fun copyFile(
        fileSystemResolver: FileSystemResolver,
        source: FileDescriptor,
        destination: FileDescriptor
    ): Either<OperationError, Unit> =
        either {
            val sourceFsProvider = fileSystemResolver.resolveProvider(source.fsAuthority)
            val destinationFsProvider = fileSystemResolver.resolveProvider(destination.fsAuthority)

            val input = sourceFsProvider.openFileForRead(
                source,
                OnConflictStrategy.CANCEL,
                FSOptions.NO_CACHE
            ).toEither().bind()

            val output = destinationFsProvider.openFileForWrite(
                destination,
                OnConflictStrategy.CANCEL,
                FSOptions.NO_CACHE
            ).toEither()
                .onLeft { InputOutputUtils.close(input) }
                .bind()

            InputOutputUtils.copy(input, output, true).toEither().bind()
        }
}