package com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.MergeFiles
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.extensions.toEither
import kotlinx.coroutines.withContext

class ResolveConflictDialogInteractor(
    private val dbRepository: EncryptedDatabaseRepository,
    private val getDatabaseUseCase: GetDatabaseUseCase,
    private val syncUseCases: SyncUseCases,
    private val dispatchers: DispatcherProvider
) {

    fun isDatabaseOpened(): Boolean =
        dbRepository.isOpened()

    fun getOpenedDatabaseKey(): Either<OperationError, EncryptedDatabaseKey> =
        either {
            val db = getDatabaseUseCase.getDatabase().toEither().bind()

            db.getKey()
        }

    suspend fun getSyncConflictInfo(
        file: FileDescriptor
    ): Either<OperationError, SyncConflictInfo> =
        withContext(dispatchers.IO) {
            syncUseCases.getSyncConflictInfo(file).toEither()
        }

    suspend fun resolveConflict(
        file: FileDescriptor,
        requestedResolution: RequestedSyncResolution
    ): Either<OperationError, FileDescriptor> =
        withContext(dispatchers.IO) {
            syncUseCases.resolveConflict(file, requestedResolution).toEither()
        }

    suspend fun getMergeFiles(
        file: FileDescriptor
    ): Either<OperationError, MergeFiles> =
        withContext(dispatchers.IO) {
            syncUseCases.getMergeFiles(file)
        }
}