package com.ivanovsky.passnotes.data.repository.file.git

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.GitRoot
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_INVALID_DATABASE_ENTRY
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_GET_PARENT_FOR
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FILE_IS_NOT_A_DIRECTORY
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.db.dao.GitRootDao
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.file.git.model.SshKey
import com.ivanovsky.passnotes.data.repository.file.git.model.VersionedFile
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClientV2
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.getUrl
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.extensions.toFileDescriptor
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.UrlUtils.SCHEME_SSH
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import timber.log.Timber

class GitClient(
    private val fileSystemResolver: FileSystemResolver,
    private val authenticator: GitFileSystemAuthenticator,
    private val fileHelper: FileHelper,
    private val gitRootDao: GitRootDao,
    private val settings: Settings,
    private val resourceProvider: ResourceProvider
) : RemoteApiClientV2 {

    private val lock = ReentrantLock()

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        Timber.d("listFiles: path=%s", dir.path)
        val openRepoResult = lock.withLock { openAndUpdateGitRepository() }
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
        val openRepoResult = lock.withLock { openAndUpdateGitRepository() }
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

        val openRepoResult = lock.withLock { openAndUpdateGitRepository() }
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

        val getMetadataResult = lock.withLock {
            val openRepoResult = openAndUpdateGitRepository()
            if (openRepoResult.isFailed) {
                return@withLock openRepoResult.mapError()
            }

            val repository = openRepoResult.obj
            val getMetadataResult = repository.getFileMetadata(file)
            if (getMetadataResult.isFailed) {
                return@withLock getMetadataResult.mapError()
            }

            getMetadataResult
        }
        if (getMetadataResult.isFailed) {
            return getMetadataResult.mapError()
        }

        val metadata = getMetadataResult.obj
        Timber.d(
            "getFileMetadata(result): local=%s, server=%s, client=%s, revision=%s",
            file.modified,
            metadata.serverModified,
            metadata.clientModified,
            metadata.revision
        )

        return getMetadataResult
    }

    override fun downloadFile(
        remotePath: String,
        destinationPath: String
    ): OperationResult<RemoteFileMetadata> {
        Timber.d("downloadFile: path=%s", remotePath)
        return lock.withLock {
            val openRepoResult = openAndUpdateGitRepository()
            if (openRepoResult.isFailed) {
                return@withLock openRepoResult.mapError()
            }

            val repository = openRepoResult.obj
            val localRoot = repository.root
            val localFile = File(localRoot, remotePath)
            if (!localFile.exists()) {
                return@withLock OperationResult.error(failedToFindFile(localFile.path))
            }

            val copyResult = InputOutputUtils.copy(
                sourceFile = localFile,
                destinationFile = File(destinationPath)
            )
            if (copyResult.isFailed) {
                return@withLock copyResult.mapError()
            }

            repository.getFileMetadata(localFile.toFileDescriptor(repository))
        }
    }

    override fun uploadFile(
        remotePath: String,
        localPath: String
    ): OperationResult<RemoteFileMetadata> {
        Timber.d("uploadFile: path=%s", remotePath)
        return lock.withLock {
            val openRepoResult = openAndUpdateGitRepository()
            if (openRepoResult.isFailed) {
                return@withLock openRepoResult.mapError()
            }

            val repository = openRepoResult.obj

            val fetchResult = repository.fetch()
            if (fetchResult.isFailed) {
                return@withLock fetchResult.mapError()
            }

            val isUpToDateResult = repository.isUpToDate()
            if (isUpToDateResult.isFailed) {
                return@withLock isUpToDateResult.mapError()
            }

            val versionedFile = VersionedFile(localPath = remotePath.removePrefix("/"))
            val isUpToDate = isUpToDateResult.obj
            if (!isUpToDate) {
                val pullResult = repository.pull(
                    file = versionedFile,
                    changedFile = File(localPath)
                )
                if (pullResult.isFailed) {
                    return@withLock pullResult.mapError()
                }
            }

            val localFile = File(repository.root, remotePath)
            if (!localFile.exists()) {
                return@withLock OperationResult.error(failedToFindFile(localFile.path))
            }

            val copyResult = InputOutputUtils.copy(
                sourceFile = File(localPath),
                destinationFile = localFile
            )
            if (copyResult.isFailed) {
                return@withLock copyResult.mapError()
            }

            val addResult = repository.addToIndex(versionedFile)
            if (addResult.isFailed) {
                return@withLock addResult.mapError()
            }

            val commitResult = repository.commit(
                message = formatCommitMessage(versionedFile),
                userName = settings.gitUserName ?: getDefaultUserName(),
                userEmail = settings.gitUserEmail ?: getDefaultUserEmail()
            )
            if (commitResult.isFailed) {
                return@withLock commitResult.mapError()
            }

            val pushResult = repository.push()
            if (pushResult.isFailed) {
                return@withLock pushResult.mapError()
            }

            repository.getFileMetadata(localFile.toFileDescriptor(repository))
        }
    }

    private fun openAndUpdateGitRepository(): OperationResult<GitRepository> {
        val fsAuthority = authenticator.getFsAuthority()

        val credentials = fsAuthority.credentials
            ?: return OperationResult.error(invalidCredentials())

        val url = credentials.getUrl()

        val gitEntry = gitRootDao.getAll()
            .firstOrNull { it.fsAuthority == fsAuthority }

        val sshCredentials = (fsAuthority.credentials as? FSCredentials.SshCredentials)
        val isSsh = (sshCredentials != null)

        if (gitEntry != null) {
            val root = File(gitEntry.path)

            val sshKey = if (isSsh && sshCredentials != null) {
                val readSshKeyResult = readSshKey(gitEntry, sshCredentials)
                if (readSshKeyResult.isFailed) {
                    return readSshKeyResult.mapError()
                }

                readSshKeyResult.getOrThrow()
            } else {
                null
            }

            val repositoryResult = GitRepository.open(
                dir = root,
                sshKey = sshKey
            )
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

        val sshKey = if (isSsh && sshCredentials != null) {
            val sshKeyFile = sshCredentials.keyFile
            val copyKeyResult = copyKeyToLocalStorage(sshKeyFile.toFileDescriptor())
            if (copyKeyResult.isFailed) {
                return copyKeyResult.mapError()
            }

            SshKey(
                keyPath = copyKeyResult.getOrThrow().path,
                password = sshCredentials.password
            )
        } else {
            null
        }

        val root = newRootPathResult.obj
        val cloneResult = GitRepository.clone(
            url = fixUrlIfNeed(url),
            dir = root,
            sshKey = sshKey
        )
        if (cloneResult.isFailed) {
            return cloneResult.mapError()
        }

        gitRootDao.insert(
            GitRoot(
                fsAuthority = fsAuthority,
                path = root.path,
                sshKeyPath = sshKey?.keyPath,
                sshKeyFile = sshCredentials?.keyFile
            )
        )

        return cloneResult
    }

    private fun fixUrlIfNeed(url: String): String {
        return if (url.startsWith(SCHEME_SSH)) {
            url.removePrefix(SCHEME_SSH)
        } else {
            url
        }
    }

    private fun readSshKey(
        entry: GitRoot,
        credentials: FSCredentials.SshCredentials
    ): OperationResult<SshKey> {
        val sshKeyPath = entry.sshKeyPath
            ?: return OperationResult.error(invalidGitEntry(entry.toString()))

        if (!File(sshKeyPath).exists()) {
            return OperationResult.error(failedToFindFile(sshKeyPath))
        }

        return OperationResult.success(
            SshKey(
                keyPath = sshKeyPath,
                password = credentials.password
            )
        )
    }

    private fun copyKeyToLocalStorage(
        keyFile: FileDescriptor
    ): OperationResult<File> {
        val fsProvider = fileSystemResolver.resolveProvider(keyFile.fsAuthority)

        val getKeyContent = fsProvider.openFileForRead(
            keyFile,
            OnConflictStrategy.CANCEL,
            FSOptions.READ_ONLY
        )
        if (getKeyContent.isFailed) {
            return getKeyContent.mapError()
        }

        val content = getKeyContent.getOrThrow()

        val generateDestinationResult = fileHelper.generateDestinationForPrivateFile(
            name = keyFile.name
        )
        if (generateDestinationResult.isFailed) {
            return generateDestinationResult.mapError()
        }

        val localKeyFile = generateDestinationResult.getOrThrow()
        val copyResult = InputOutputUtils.copy(
            source = content,
            destinationFile = localKeyFile
        )

        return copyResult.mapWithObject(localKeyFile)
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

    private fun formatCommitMessage(
        file: VersionedFile
    ): String {
        val appName = resourceProvider.getString(R.string.app_name)
        return "Update ${file.name}. Committed with $appName"
    }

    private fun getDefaultUserName(): String {
        return resourceProvider.getString(R.string.app_name)
    }

    private fun getDefaultUserEmail(): String {
        val appName = resourceProvider.getString(R.string.app_name)
        return "${appName.lowercase()}@localhost"
    }

    private fun invalidCredentials(): OperationError {
        return newGenericError(MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS, Stacktrace())
    }

    private fun invalidGitEntry(entry: String): OperationError {
        return newGenericError(
            String.format(
                GENERIC_INVALID_DATABASE_ENTRY,
                entry
            ),
            Stacktrace()
        )
    }

    private fun failedToFindFile(path: String): OperationError {
        return newGenericIOError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_FIND_FILE,
                path
            ),
            Stacktrace()
        )
    }

    private fun failedToGetParent(path: String): OperationError {
        return newGenericIOError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_GET_PARENT_FOR,
                path
            ),
            Stacktrace()
        )
    }

    private fun fileIsNotDirectory(path: String): OperationError {
        return newGenericIOError(
            String.format(
                GENERIC_MESSAGE_FILE_IS_NOT_A_DIRECTORY,
                path
            ),
            Stacktrace()
        )
    }

    companion object {
        private const val GIT_DIRECTORY_NAME = ".git"
    }
}