package com.ivanovsky.passnotes.presentation.diffViewer

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNSUPPORTED_DATABASE_TYPE
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RequestedSyncResolution
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.ApplyDiffUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.GetDiffUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.presentation.diffViewer.model.CompareData
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffEntity
import com.ivanovsky.passnotes.presentation.diffViewer.model.MergeData
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.FileUtils.copyFile
import kotlinx.coroutines.withContext

class DiffViewerInteractor(
    private val settings: Settings,
    private val dispatchers: DispatcherProvider,
    private val dbRepository: EncryptedDatabaseRepository,
    private val fileSystemResolver: FileSystemResolver,
    private val getDbUseCase: GetDatabaseUseCase,
    private val diffUseCase: GetDiffUseCase,
    private val applyDiffUseCase: ApplyDiffUseCase
) {

    suspend fun loadMergeData(
        mode: DiffViewerMode.Merge
    ): Either<OperationError, MergeData> =
        withContext(dispatchers.IO) {
            either {
                val base = dbRepository.read(
                    KeepassImplementation.KOTPASS,
                    mode.key,
                    mode.base,
                    FSOptions.READ_ONLY
                ).toEither().bind() as KotpassDatabase

                val local = dbRepository.read(
                    KeepassImplementation.KOTPASS,
                    mode.key,
                    mode.local,
                    FSOptions.READ_ONLY
                ).toEither().bind() as KotpassDatabase

                val remote = dbRepository.read(
                    KeepassImplementation.KOTPASS,
                    mode.key,
                    mode.remote,
                    FSOptions.READ_ONLY
                ).toEither().bind() as KotpassDatabase

                val localDiff = getDiff(
                    lhs = base,
                    rhs = local
                ).bind()

                val remoteDiff = getDiff(
                    lhs = base,
                    rhs = remote
                ).bind()

                MergeData(
                    base = base,
                    local = local,
                    remote = remote,
                    localDiff = localDiff,
                    remoteDiff = remoteDiff
                )
            }
        }

    suspend fun loadCompareData(
        mode: DiffViewerMode.Compare
    ): Either<OperationError, CompareData> =
        withContext(dispatchers.IO) {
            either {
                val left = loadDatabaseAndFile(mode.left).bind()
                val right = loadDatabaseAndFile(mode.right).bind()

                val diff = getDiff(
                    lhs = left.db,
                    rhs = right.db
                ).bind()

                CompareData(
                    left = left.db,
                    right = right.db,
                    diff = diff
                )
            }
        }

    suspend fun applyDiff(
        key: EncryptedDatabaseKey,
        base: FileDescriptor,
        output: FileDescriptor,
        diff: List<DiffEvent<EncryptedDatabaseElement>>
    ): Either<OperationError, Unit> =
        withContext(dispatchers.IO) {
            either {
                val tempOutput = FileUtils.createTemporalFile(
                    fileSystemResolver = fileSystemResolver,
                    source = output
                ).bind()

                copyFile(
                    fileSystemResolver = fileSystemResolver,
                    source = base,
                    destination = tempOutput
                ).bind()

                val db = dbRepository.read(
                    settings.keepassImplementation,
                    key,
                    tempOutput,
                    FSOptions.NO_CACHE
                ).toEither().bind()

                val result = applyDiffUseCase.applyDiff(
                    db = db,
                    diff = diff
                ).bind()

                val fsProvider = fileSystemResolver.resolveProvider(output.fsAuthority)
                result.commitTo(
                    output.copy(modified = System.currentTimeMillis()),
                    FSOptions.CACHE_ONLY
                ).bind()

                fsProvider.syncProcessor.process(
                    file = output.copy(),
                    requestedResolution = RequestedSyncResolution.UPLOAD_LOCAL_FILE
                ).toEither().bind()

                dbRepository.reload().toEither().bind()
            }
        }

    private fun loadDatabaseAndFile(
        entity: DiffEntity
    ): Either<OperationError, DatabaseAndFile> =
        either {
            when (entity) {
                is DiffEntity.OpenedDatabase -> getOpenedDatabaseAndFile().bind()

                is DiffEntity.File -> {
                    val db = dbRepository.read(
                        KeepassImplementation.KOTPASS,
                        entity.key,
                        entity.file,
                        FSOptions.READ_ONLY
                    ).toEither().bind()

                    DatabaseAndFile(
                        db = db as KotpassDatabase,
                        file = entity.file
                    )
                }
            }
        }

    private fun readDatabase(
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ): OperationResult<KotpassDatabase> {
        val readResult = dbRepository.read(
            KeepassImplementation.KOTPASS,
            key,
            file,
            FSOptions.READ_ONLY
        )
        if (readResult.isFailed) {
            return readResult.mapError()
        }

        val db = readResult.getOrThrow()

        return if (db is KotpassDatabase) {
            OperationResult.success(db)
        } else {
            OperationResult.error(
                newGenericError(
                    MESSAGE_UNSUPPORTED_DATABASE_TYPE,
                    Stacktrace()
                )
            )
        }
    }

    private fun getOpenedDatabaseAndFile(): Either<OperationError, DatabaseAndFile> =
        either {
            val openedDb = getDbUseCase.getDatabaseSynchronously().toEither().bind()
            val fsProvider = fileSystemResolver.resolveProvider(openedDb.getFile().fsAuthority)

            val db = dbRepository.read(
                KeepassImplementation.KOTPASS,
                openedDb.getKey(),
                openedDb.getFile(),
                FSOptions.READ_ONLY
            ).toEither().bind()

            val file = fsProvider.getFile(
                openedDb.getFile().path,
                FSOptions.READ_ONLY
            ).toEither().bind()

            DatabaseAndFile(
                db = db as KotpassDatabase,
                file = file
            )
        }

    private suspend fun getDiff(
        lhs: KotpassDatabase,
        rhs: KotpassDatabase
    ): Either<OperationError, List<DiffListItem>> =
        diffUseCase.getDiff(lhs, rhs).right()

    data class DatabaseAndFile(
        val db: KotpassDatabase,
        val file: FileDescriptor
    )
}