package com.ivanovsky.passnotes.presentation.diffViewer

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNSUPPORTED_DATABASE_TYPE
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.GetDiffUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError

class DiffViewerInteractor(
    private val dispatchers: DispatcherProvider,
    private val dbRepository: EncryptedDatabaseRepository,
    private val fileSystemResolver: FileSystemResolver,
    private val getDbUseCase: GetDatabaseUseCase,
    private val diffUseCase: GetDiffUseCase
) {

    suspend fun getOpenedDatabaseAndFile():
        OperationResult<Pair<KotpassDatabase, FileDescriptor>> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabaseSynchronously()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val db = getDbResult.getOrThrow()

            val fsProvider = fileSystemResolver.resolveProvider(db.file.fsAuthority)
            val getFileResult = fsProvider.getFile(db.file.path, FSOptions.READ_ONLY)
            if (getFileResult.isFailed) {
                return@withContext getFileResult.mapError()
            }

            val file = getFileResult.getOrThrow()

            if (db is KotpassDatabase) {
                OperationResult.success(Pair(db, file))
            } else {
                OperationResult.error(newGenericError(MESSAGE_UNSUPPORTED_DATABASE_TYPE))
            }
        }

    suspend fun readDatabase(
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ): OperationResult<KotpassDatabase> =
        withContext(dispatchers.IO) {
            val readResult = dbRepository.read(
                KeepassImplementation.KOTPASS,
                key,
                file
            )
            if (readResult.isFailed) {
                return@withContext readResult.mapError()
            }

            val db = readResult.getOrThrow()

            if (db is KotpassDatabase) {
                OperationResult.success(db)
            } else {
                OperationResult.error(newGenericError(MESSAGE_UNSUPPORTED_DATABASE_TYPE))
            }
        }

    suspend fun getDiff(
        lhs: KotpassDatabase,
        rhs: KotpassDatabase
    ): OperationResult<List<DiffListItem>> =
        withContext(dispatchers.Default) {
            OperationResult.success(diffUseCase.getDiff(lhs, rhs))
        }
}
