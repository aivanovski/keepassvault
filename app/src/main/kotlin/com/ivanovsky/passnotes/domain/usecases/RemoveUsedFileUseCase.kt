package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.dao.GitRootDao
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import java.io.File
import kotlinx.coroutines.withContext
import timber.log.Timber

class RemoveUsedFileUseCase(
    private val fileRepository: UsedFileRepository,
    private val gitRootDao: GitRootDao,
    private val dispatchers: DispatcherProvider
) {

    suspend fun removeUsedFile(uid: String, fsAuthority: FSAuthority): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val usedFile = fileRepository.findByUid(uid, fsAuthority)
                ?: return@withContext OperationResult.error(
                    newDbError(
                        String.format(
                            GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            UsedFile::class.simpleName,
                            uid
                        ),
                        Stacktrace()
                    )
                )

            if (fsAuthority.type == FSType.GIT) {
                removeSshKeyIfNeed(fsAuthority)
                removeGitEntryIfNeed(fsAuthority)
            }

            if (usedFile.id != null) {
                Timber.d("Remove used file: id=%s", usedFile.id)
                fileRepository.remove(usedFile.id)
            }

            OperationResult.success(true)
        }

    private fun removeGitEntryIfNeed(fsAuthority: FSAuthority) {
        val gitEntry = gitRootDao.getAll().firstOrNull { entry ->
            entry.fsAuthority == fsAuthority
        } ?: return

        if (gitEntry.id != null) {
            Timber.d("Remove git entry: id=%s", gitEntry.id)
            gitRootDao.remove(gitEntry.id)
        }
    }

    private fun removeSshKeyIfNeed(fsAuthority: FSAuthority) {
        val gitEntry = gitRootDao.getAll().firstOrNull { entry ->
            entry.fsAuthority == fsAuthority
        } ?: return

        val keyFilePath = gitEntry.sshKeyPath
        if (keyFilePath != null) {
            val keyFile = File(keyFilePath)
            if (keyFile.exists()) {
                Timber.d("Remove ssh key file: %s", keyFilePath)
                keyFile.delete()
            }
        }
    }
}