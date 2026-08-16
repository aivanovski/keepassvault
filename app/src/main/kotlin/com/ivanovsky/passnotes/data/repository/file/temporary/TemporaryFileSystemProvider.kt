package com.ivanovsky.passnotes.data.repository.file.temporary

import android.content.Context
import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_ACCESS_IS_FORBIDDEN
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newInvalidFileSystemProviderUsage
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.TemporaryFile
import com.ivanovsky.passnotes.data.repository.TemporaryFileRepository
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.util.toOperationResult
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class TemporaryFileSystemProvider(
    context: Context,
    private val fileRepository: TemporaryFileRepository,
    private val fsAuthority: FSAuthority
) : FileSystemProvider {

    private val root = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }
    private val provider = RegularFileSystemProvider(context, fsAuthority)

    override val authenticator = provider.authenticator
    override val syncProcessor = provider.syncProcessor

    override fun listFiles(
        dir: FileDescriptor
    ): OperationResult<List<FileDescriptor>> =
        OperationResult.error(newInvalidFileSystemProviderUsage(Stacktrace()))

    override fun getParent(
        file: FileDescriptor
    ): OperationResult<FileDescriptor> =
        OperationResult.error(newInvalidFileSystemProviderUsage(Stacktrace()))

    override fun getRootFile(): OperationResult<FileDescriptor> =
        OperationResult.success(root.toFileDescriptor())

    override fun exists(
        file: FileDescriptor
    ): OperationResult<Boolean> =
        either {
            checkInsideRoot(file.path).bind()

            provider.exists(file).toEither().bind()
        }.toOperationResult()

    override fun getFile(
        path: String,
        options: FSOptions
    ): OperationResult<FileDescriptor> =
        provider.getFile(path, options)

    override fun openFileForRead(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<InputStream> =
        either {
            checkInsideRoot(file.path).bind()

            provider.openFileForRead(file, onConflictStrategy, options).toEither().bind()
        }.toOperationResult()

    override fun openFileForWrite(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<OutputStream> =
        either {
            checkInsideRoot(file.path).bind()

            val output =
                provider.openFileForWrite(file, onConflictStrategy, options).toEither().bind()

            fileRepository.insert(
                TemporaryFile(
                    path = file.path,
                    created = System.currentTimeMillis(),
                    modified = file.modified
                )
            )

            output
        }.toOperationResult()

    private fun checkInsideRoot(path: String): Either<OperationError, Unit> =
        either {
            val isInside = (path == root.path || path.startsWith(root.path))

            if (!isInside) {
                raise(newFileAccessError(MESSAGE_FILE_ACCESS_IS_FORBIDDEN, Stacktrace()))
            }
        }

    private fun File.toFileDescriptor(): FileDescriptor {
        return FileDescriptor(
            fsAuthority = fsAuthority,
            path = path,
            uid = path,
            name = name,
            isDirectory = isDirectory,
            isRoot = isRoot(),
            modified = lastModified()
        )
    }

    private fun File.isRoot(): Boolean {
        return path == root.path
    }

    companion object {
        private const val DIRECTORY_NAME = "temporary_files"
    }
}