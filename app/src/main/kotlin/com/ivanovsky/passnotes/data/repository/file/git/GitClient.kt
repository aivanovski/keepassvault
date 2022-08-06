package com.ivanovsky.passnotes.data.repository.file.git

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.GitRoot
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_GET_PARENT_FOR
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FILE_IS_NOT_A_DIRECTORY
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.db.dao.GitRootDao
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClientV2
import com.ivanovsky.passnotes.data.repository.file.git.model.VersionedFile
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.InputOutputUtils
import timber.log.Timber
import java.io.File

class GitClient(
    private val authenticator: GitFileSystemAuthenticator,
    private val fileHelper: FileHelper,
    private val gitRootDao: GitRootDao
) : RemoteApiClientV2 {

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        Timber.d("listFiles: path=%s", dir.path)

        val openRepoResult = openAndUpdateGitRepository()
        if (openRepoResult.isFailed) {
            return openRepoResult.mapError()
        }

        val repository = openRepoResult.obj
        val localDir = File(repository.root, dir.path)
        if (!localDir.exists()) {
            return OperationResult.error(failedToFindFile(dir.path))
        }
        if (!localDir.isDirectory) {
            return OperationResult.error(fileIsNotDirectory(dir.path))
        }

        val dirLocalFiles = localDir.listFiles() ?: emptyArray()
        val dirFiles = dirLocalFiles
            .filter { !it.isDirectory || it.name != GIT_DIRECTORY_NAME }
            .map { it.toFileDescriptor(repository) }

        return OperationResult.success(dirFiles)
    }

    override fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
        Timber.d("getParent: path=%s", file.path)

        val openRepoResult = openAndUpdateGitRepository()
        if (openRepoResult.isFailed) {
            return openRepoResult.mapError()
        }

        val repository = openRepoResult.obj
        val localFile = File(repository.root, file.path)
        if (!localFile.exists()) {
            return OperationResult.error(failedToFindFile(file.path))
        }

        val parentFile = localFile.parentFile
            ?: return OperationResult.error(failedToGetParent(file.path))

        return OperationResult.success(parentFile.toFileDescriptor(repository))
    }

    override fun getRoot(): OperationResult<FileDescriptor> {
        Timber.d("getRoot:")

        val openRepoResult = openAndUpdateGitRepository()
        if (openRepoResult.isFailed) {
            return openRepoResult.mapError()
        }

        val repository = openRepoResult.obj
        val localRoot = repository.root
        return openRepoResult.mapWithObject(
            localRoot.toFileDescriptor(repository)
        )
    }

    override fun getFileMetadata(file: FileDescriptor): OperationResult<RemoteFileMetadata> {
        Timber.d("getFileMetadata: path=%s", file.path)

        val openRepoResult = openAndUpdateGitRepository()
        if (openRepoResult.isFailed) {
            return openRepoResult.mapError()
        }

        val repository = openRepoResult.obj
        val getMetadataResult = repository.getFileMetadata(file)
        if (getMetadataResult.isFailed) {
            return getMetadataResult.mapError()
        }

        val metadata = getMetadataResult.obj
        Timber.d(
            "getFileMetadata(result): localModified=%s, serverModified=%s, clientModified=%s, revision=%s",
            file.modified,
            metadata.serverModified,
            metadata.clientModified,
            metadata.revision)

        return getMetadataResult
    }

    override fun downloadFile(
        remotePath: String,
        destinationPath: String
    ): OperationResult<RemoteFileMetadata> {
        Timber.d("downloadFile: path=%s", remotePath)

        val openRepoResult = openAndUpdateGitRepository()
        if (openRepoResult.isFailed) {
            return openRepoResult.mapError()
        }

        val repository = openRepoResult.obj
        val localRoot = repository.root
        val localFile = File(localRoot, remotePath)
        if (!localFile.exists()) {
            return OperationResult.error(failedToFindFile(localFile.path))
        }

        val copyResult = InputOutputUtils.copy(
            sourceFile = localFile,
            destinationFile = File(destinationPath)
        )
        if (copyResult.isFailed) {
            return copyResult.mapError()
        }

        return repository.getFileMetadata(localFile.toFileDescriptor(repository))
    }

    override fun uploadFile(
        remotePath: String,
        localPath: String
    ): OperationResult<RemoteFileMetadata> {
        Timber.d("uploadFile: path=%s", remotePath)

        val openRepoResult = openAndUpdateGitRepository()
        if (openRepoResult.isFailed) {
            return openRepoResult.mapError()
        }

        val repository = openRepoResult.obj

        val fetchResult = repository.fetch()
        if (fetchResult.isFailed) {
            return fetchResult.mapError()
        }

        val isUpToDateResult = repository.isUpToDate()
        if (isUpToDateResult.isFailed) {
            return isUpToDateResult.mapError()
        }

        val versionedFile = VersionedFile(localPath = remotePath.removePrefix("/"))
        val isUpToDate = isUpToDateResult.obj
        if (!isUpToDate) {
            val pullResult = repository.pull(
                file = versionedFile,
                changedFile = File(localPath)
            )
            if (pullResult.isFailed) {
                return pullResult.mapError()
            }
        }

        val localFile = File(repository.root, remotePath)
        if (!localFile.exists()) {
            return OperationResult.error(failedToFindFile(localFile.path))
        }

        val copyResult = InputOutputUtils.copy(
            sourceFile = File(localPath),
            destinationFile = localFile
        )
        if (copyResult.isFailed) {
            return copyResult.mapError()
        }

        val addResult = repository.addToIndex(versionedFile)
        if (addResult.isFailed) {
            return addResult.mapError()
        }

        val commitResult = repository.commit(versionedFile)
        if (commitResult.isFailed) {
            return commitResult.mapError()
        }

        val pushResult = repository.push()
        if (pushResult.isFailed) {
            return pushResult.mapError()
        }

        return repository.getFileMetadata(localFile.toFileDescriptor(repository))
    }

    private fun openAndUpdateGitRepository(): OperationResult<GitRepository> {
        val fsAuthority = authenticator.getFsAuthority()

        val gitRoot = gitRootDao.getAll()
            .firstOrNull { it.fsAuthority == fsAuthority }

        if (gitRoot != null) {
            val root = File(gitRoot.path)
            val repositoryResult = GitRepository.open(root)
            if (repositoryResult.isFailed) {
                return repositoryResult.mapError()
            }

            val repository = repositoryResult.obj
            val fetchResult = repository.fetch()
            if (fetchResult.isFailed) {
                return fetchResult.mapError()
            }

            val isUpToDateResult = repository.isUpToDate()
            if (isUpToDateResult.isFailed) {
                return isUpToDateResult.mapError()
            }

            val isUpToDate = isUpToDateResult.obj
            if (!isUpToDate) {
                val pullResult = repository.pull()
                if (pullResult.isFailed) {
                    return pullResult.mapError()
                }
            }

            return repositoryResult
        }

        val newRootPathResult = fileHelper.generateDestinationFile()
        if (newRootPathResult.isFailed) {
            return newRootPathResult.mapError()
        }

        val root = newRootPathResult.obj
        val cloneResult = GitRepository.clone(root, authenticator.getFsAuthority())
        if (cloneResult.isFailed) {
            return cloneResult.mapError()
        }

        gitRootDao.insert(
            GitRoot(
                fsAuthority = authenticator.getFsAuthority(),
                path = root.path
            )
        )

        return cloneResult
    }

    private fun File.toFileDescriptor(repository: GitRepository): FileDescriptor {
        val localRootPath = repository.root.path
        val isRoot = (localRootPath == path)
        val fileSystemPath = when {
            isRoot -> FileUtils.ROOT_PATH
            path.startsWith(localRootPath) && path.length > localRootPath.length -> {
                path.removePrefix(localRootPath)
            }
            else -> throw IllegalArgumentException()
        }
        return FileDescriptor(
            fsAuthority = authenticator.getFsAuthority(),
            path = fileSystemPath,
            uid = fileSystemPath,
            name = if (isRoot) FileUtils.ROOT_PATH else name,
            isDirectory = isDirectory,
            isRoot = isRoot,
            modified = lastModified()
        )
    }

    private fun failedToFindFile(path: String): OperationError {
        return newGenericIOError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_FIND_FILE,
                path
            )
        )
    }

    private fun failedToGetParent(path: String): OperationError {
        return newGenericIOError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_GET_PARENT_FOR,
                path
            )
        )
    }

    private fun fileIsNotDirectory(path: String): OperationError {
        return newGenericIOError(
            String.format(
                GENERIC_MESSAGE_FILE_IS_NOT_A_DIRECTORY,
                path
            )
        )
    }

    companion object {
        private const val GIT_DIRECTORY_NAME = ".git"
    }
}