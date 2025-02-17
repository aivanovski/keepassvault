package com.ivanovsky.passnotes.data.repository.file.git

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_GET_REFERENCE_TO
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationError.newNetworkIOError
import com.ivanovsky.passnotes.data.entity.OperationError.newRemoteApiError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.file.git.model.SshKey
import com.ivanovsky.passnotes.data.repository.file.git.model.VersionedFile
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.map
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.File
import java.io.IOException
import java.util.Date
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS
import timber.log.Timber

class GitRepository(
    private val sshKey: SshKey?,
    private val git: Git,
    val root: File
) {

    // TODO(improvement): Some operations may produce conflicts in local repository,
    //  they should be resolved then

    fun getFileMetadata(file: FileDescriptor): OperationResult<RemoteFileMetadata> {
        val getLocalHeadIdResult = getLocalHeadId()
        if (getLocalHeadIdResult.isFailed) {
            return getLocalHeadIdResult.mapError()
        }

        val localHeadId = getLocalHeadIdResult.obj.name
        val localFile = File(root, file.path)
        if (!localFile.exists()) {
            return OperationResult.error(failedToFindFile(file.path))
        }

        val logResult = execute { git.log().call() }
        if (logResult.isFailed) {
            return logResult.mapError()
        }

        // TODO(improvement): Modification time should be defined by last commit that modifies file
        val lastCommit = logResult.obj.firstOrNull()
        val modified = if (lastCommit != null) {
            Date(lastCommit.commitTime * 1000L)
        } else {
            Date(localFile.lastModified())
        }

        return OperationResult.success(
            RemoteFileMetadata(
                uid = file.path,
                path = file.path,
                serverModified = modified,
                clientModified = modified,
                revision = localHeadId
            )
        )
    }

    fun isUpToDate(): OperationResult<Boolean> {
        val getLocalHeadId = getLocalHeadId()
        val getRemoteHeadId = getRemoteHeadId()
        if (getLocalHeadId.isFailed) {
            return getLocalHeadId.mapError()
        }
        if (getRemoteHeadId.isFailed) {
            return getRemoteHeadId.mapError()
        }

        val localHeadId = getLocalHeadId.obj
        val remoteHeadId = getRemoteHeadId.obj
        val isUpToDate = (
            localHeadId != null &&
                remoteHeadId != null &&
                localHeadId == remoteHeadId
            )

        return OperationResult.success(isUpToDate)
    }

    fun isWorkingTreeClean(): OperationResult<Boolean> {
        val getStatusResult = execute {
            git.status().call()
        }
        if (getStatusResult.isFailed) {
            return getStatusResult.mapError()
        }

        val status = getStatusResult.obj
        val isClean = status.uncommittedChanges.isEmpty() &&
            status.added.isEmpty() &&
            status.isClean

        return OperationResult.success(isClean)
    }

    fun pull(
        file: VersionedFile? = null,
        changedFile: File? = null
    ): OperationResult<Unit> {
        for (pullIdx in 0..1) {
            val pullResult = execute {
                git.pull()
                    .apply {
                        setRebase(true)
                        setRebase(BranchConfig.BranchRebaseMode.REBASE)
                        if (sshKey != null) {
                            setTransportConfigCallback(createSshTransportCallback(sshKey))
                        }
                    }
                    .call()
            }
            if (pullResult.isFailed) {
                return pullResult.mapError()
            }

            val pull = pullResult.obj
            val pullStatus = pull.rebaseResult?.status
            if (pullStatus == RebaseResult.Status.FAST_FORWARD ||
                pullStatus == RebaseResult.Status.UP_TO_DATE
            ) {
                if (pullIdx == 1 && file != null && changedFile != null) {
                    val restoreBackupResult = InputOutputUtils.copy(
                        sourceFile = changedFile,
                        destinationFile = file.toFile()
                    )
                    if (restoreBackupResult.isFailed) {
                        return restoreBackupResult.mapError()
                    }
                }

                return OperationResult.success(Unit)
            }

            if (pullIdx == 0) {
                val resetResult = resetToLocalHead()
                if (resetResult.isFailed) {
                    return resetResult.mapError()
                }
            }
        }

        return OperationResult.error(newRemoteApiError(ERROR_FAILED_TO_PULL, Stacktrace()))
    }

    fun addToIndex(file: VersionedFile): OperationResult<Unit> {
        val getStatusResult = execute {
            git.status().call()
        }
        if (getStatusResult.isFailed) {
            return getStatusResult.mapError()
        }

        val status = getStatusResult.obj
        if (!status.uncommittedChanges.contains(file.localPath) &&
            !status.added.contains(file.localPath)
        ) {
            return OperationResult.error(newRemoteApiError(ERROR_NOTHING_TO_COMMIT, Stacktrace()))
        }

        val addResult = execute {
            git.add()
                .addFilepattern(file.localPath)
                .call()
        }

        return addResult.mapWithObject(Unit)
    }

    fun commit(
        message: String,
        userName: String,
        userEmail: String
    ): OperationResult<Unit> {
        val author = PersonIdent(userName, userEmail)
        return execute {
            git.commit()
                .setMessage(message)
                .setAuthor(author)
                .setCommitter(author)
                .call()
        }
    }

    fun push(): OperationResult<Unit> {
        val pushResult = execute {
            git.push()
                .apply {
                    if (sshKey != null) {
                        setTransportConfigCallback(createSshTransportCallback(sshKey))
                    }
                }
                .call()
        }
        if (pushResult.isFailed) {
            return pushResult.mapError()
        }

        val push = pushResult.obj
        if (push.any { !it.isSuccessful() }) {
            return OperationResult.error(newRemoteApiError(ERROR_FAILED_TO_PUSH, Stacktrace()))
        }

        return OperationResult.success(Unit)
    }

    private fun resetToLocalHead(): OperationResult<Unit> {
        val resetResult = execute {
            git.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef(HEAD)
                .call()
        }
        if (resetResult.isFailed) {
            return resetResult.mapError()
        }

        val isCleanResult = isWorkingTreeClean()
        return isCleanResult.map { }
    }

    private fun getRemoteHeadId(): OperationResult<ObjectId> {
        if (git.repository.remoteNames.size > 1) {
            return OperationResult.error(
                newRemoteApiError(
                    ERROR_MORE_THAN_ONE_REMOTE,
                    Stacktrace()
                )
            )
        }

        val remoteName = git.repository.remoteNames.firstOrNull()
            ?: return OperationResult.error(
                newRemoteApiError(
                    ERROR_FAILED_TO_GET_ORIGIN,
                    Stacktrace()
                )
            )

        val getLocalHeadResult = getLocalHead()
        if (getLocalHeadResult.isFailed) {
            return getLocalHeadResult.mapError()
        }

        val mainName = getLocalHeadResult.obj.name.removePrefix("refs/heads/")
        val remoteHeadName = "refs/remotes/$remoteName/$mainName"
        val remoteHeadId = git.repository.refDatabase.findRef(remoteHeadName)?.target?.objectId

        return if (remoteHeadId != null) {
            OperationResult.success(remoteHeadId)
        } else {
            OperationResult.error(failedToGetReferenceTo(remoteHeadName))
        }
    }

    private fun getLocalHeadId(): OperationResult<ObjectId> {
        return getLocalHead().map { head -> head.objectId }
    }

    private fun getLocalHead(): OperationResult<Ref> {
        val localHead = git.repository.refDatabase.findRef(HEAD)?.target
        return if (localHead != null) {
            OperationResult.success(localHead)
        } else {
            OperationResult.error(failedToGetReferenceTo(HEAD))
        }
    }

    fun fetch(): OperationResult<Unit> {
        return execute {
            git.fetch()
                .apply {
                    if (sshKey != null) {
                        setTransportConfigCallback(createSshTransportCallback(sshKey))
                    }
                }
                .call()
        }
            .mapWithObject(Unit)
    }

    private fun PushResult.isSuccessful(): Boolean {
        return this.remoteUpdates.all { it.status == RemoteRefUpdate.Status.OK }
    }

    private fun failedToFindFile(path: String): OperationError {
        return newGenericIOError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE,
                path
            ),
            Stacktrace()
        )
    }

    private fun failedToGetReferenceTo(referenceName: String): OperationError {
        return newRemoteApiError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_GET_REFERENCE_TO,
                referenceName
            ),
            Stacktrace()
        )
    }

    companion object {

        private const val HEAD = "HEAD"
        private const val ERROR_NOTHING_TO_COMMIT = "Nothing to commit"
        private const val ERROR_FAILED_TO_GET_ORIGIN = "Failed to get origin"
        private const val ERROR_MORE_THAN_ONE_REMOTE = "More than 1 remote"
        private const val ERROR_FAILED_TO_PUSH = "Failed to push"
        private const val ERROR_FAILED_TO_PULL = "Failed to pull"

        fun open(
            dir: File,
            sshKey: SshKey?
        ): OperationResult<GitRepository> {
            return execute {
                Git.init()
                    .setDirectory(dir)
                    .call()
            }
                .map { git ->
                    GitRepository(
                        sshKey = sshKey,
                        git = git,
                        root = dir
                    )
                }
        }

        fun clone(
            url: String,
            dir: File,
            sshKey: SshKey?
        ): OperationResult<GitRepository> {
            val result = execute {
                val clone = if (sshKey == null) {
                    Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(dir)
                } else {
                    Git.cloneRepository()
                        .setURI(url)
                        .setTransportConfigCallback(createSshTransportCallback(sshKey))
                        .setDirectory(dir)
                }

                clone.call()
            }
            if (result.isFailed) {
                return result.mapError()
            }

            return open(dir, sshKey)
        }

        private fun createSshTransportCallback(
            sshKey: SshKey
        ): TransportConfigCallback {
            val sessionFactory = createSshSessionFactory(sshKey)

            return TransportConfigCallback { transport ->
                (transport as SshTransport).sshSessionFactory = sessionFactory
            }
        }

        private fun createSshSessionFactory(
            sshKey: SshKey
        ): JschConfigSessionFactory {
            return object : JschConfigSessionFactory() {

                override fun configure(hc: OpenSshConfig.Host, session: Session) {
                    session.setConfig("StrictHostKeyChecking", "no")
                }

                override fun createDefaultJSch(fs: FS): JSch {
                    val jsch = super.createDefaultJSch(fs)

                    if (sshKey.password != null) {
                        jsch.addIdentity(sshKey.keyPath, sshKey.password)
                    } else {
                        jsch.addIdentity(sshKey.keyPath)
                    }

                    return jsch
                }
            }
        }

        private fun <T> execute(block: () -> T): OperationResult<T> {
            return try {
                OperationResult.success(block.invoke())
            } catch (exception: Exception) {
                Timber.d(exception)
                when (exception) {
                    is TransportException, is IOException -> {
                        OperationResult.error(newNetworkIOError(exception))
                    }

                    else -> {
                        OperationResult.error(newRemoteApiError(exception.message, exception))
                    }
                }
            }
        }
    }
}