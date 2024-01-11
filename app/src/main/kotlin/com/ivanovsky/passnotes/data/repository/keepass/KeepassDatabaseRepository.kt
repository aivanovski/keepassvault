package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FSOptions.Companion.READ_ONLY
import com.ivanovsky.passnotes.data.repository.file.FSOptions.Companion.defaultOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapWithObject
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class KeepassDatabaseRepository(
    private val fileSystemResolver: FileSystemResolver,
    private val lockInteractor: DatabaseLockInteractor,
    private val syncStatusProvider: DatabaseSyncStateProvider,
    private val observerBus: ObserverBus
) : EncryptedDatabaseRepository {

    private val database = AtomicReference<DatabaseReference>()
    private val lock = ReentrantLock()

    override fun isOpened(): Boolean = (database.get() != null)

    override fun getDatabase(): EncryptedDatabase? = database.get()?.database

    override fun open(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor,
        options: FSOptions
    ): OperationResult<EncryptedDatabase> {
        val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

        val openDbResult = lock.withLock {
            if (isOpened) {
                close()
            }

            val openFileResult = fsProvider.openFileForRead(
                file,
                OnConflictStrategy.CANCEL,
                options
            )
            if (openFileResult.isFailed) {
                return@withLock openFileResult.takeError()
            }

            val openResult = openDatabase(
                type,
                fileSystemResolver,
                options,
                file,
                openFileResult,
                key
            )
            if (openResult.isFailed) {
                return@withLock openResult.takeError()
            }

            val db = openResult.obj
            database.set(DatabaseReference(type, db))
            openResult.takeStatusWith(db)
        }

        if (openDbResult.isSucceededOrDeferred) {
            onDatabaseOpened(openDbResult.getOrThrow(), openDbResult)
        }

        return openDbResult
    }

    override fun canOpen(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ): OperationResult<Unit> {
        val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

        val openFileResult = fsProvider.openFileForRead(
            file,
            OnConflictStrategy.CANCEL,
            READ_ONLY
        )
        if (openFileResult.isFailed) {
            return openFileResult.takeError()
        }

        val openResult = openDatabase(
            type,
            fileSystemResolver,
            READ_ONLY,
            file,
            openFileResult,
            key
        )

        return openResult.mapWithObject(Unit)
    }

    override fun reload(): OperationResult<Boolean> {
        val result = lock.withLock {
            if (!isOpened) {
                return@withLock OperationResult.error(newDbError(MESSAGE_FAILED_TO_GET_DATABASE))
            }

            val oldDb = database.get().database
            val type = database.get().type
            val fsProvider = fileSystemResolver.resolveProvider(oldDb.file.fsAuthority)
            val fsOptions = oldDb.fsOptions
            val file = oldDb.file
            val key = oldDb.key

            val openFileResult = fsProvider.openFileForRead(
                file,
                OnConflictStrategy.CANCEL,
                fsOptions
            )
            if (openFileResult.isFailed) {
                return@withLock openFileResult.takeError()
            }

            val openResult = openDatabase(
                type = type,
                fsResolver = fileSystemResolver,
                fsOptions = fsOptions,
                file = file,
                input = openFileResult,
                key = key
            )
            if (openResult.isFailed) {
                return@withLock openResult.takeError()
            }

            val db = openResult.obj
            database.set(DatabaseReference(type, db))
            openResult.takeStatusWith(db)
        }

        if (result.isSucceededOrDeferred) {
            observerBus.notifyDatabaseDataSetChanged()
        }

        return result.mapWithObject(true)
    }

    override fun createNew(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor,
        addTemplates: Boolean
    ): OperationResult<Boolean> {
        return lock.withLock {
            val dbResult = KotpassDatabase.new(
                fsResolver = fileSystemResolver,
                fsOptions = defaultOptions(),
                file = file,
                key = key,
                isAddTemplates = true
            )
            if (dbResult.isFailed) {
                return@withLock dbResult.takeError()
            }

            OperationResult.success(true)
        }
    }

    override fun close(): OperationResult<Boolean> {
        lock.withLock {
            if (isOpened) {
                database.get().database
                    .apply {
                        this.watcher.unsubscribe(syncStatusProvider)
                    }

                database.set(null)
            }
        }

        onDatabaseClosed()

        return OperationResult.success(true)
    }

    private fun onDatabaseOpened(
        db: EncryptedDatabase,
        openResult: OperationResult<EncryptedDatabase>
    ) {
        lockInteractor.onDatabaseOpened(db)
        syncStatusProvider.onDatabaseOpened(db, openResult)
        observerBus.notifyDatabaseOpened(db)
    }

    private fun onDatabaseClosed() {
        lockInteractor.onDatabaseClosed()
        syncStatusProvider.onDatabaseClosed()
        observerBus.notifyDatabaseClosed()
    }

    private fun openDatabase(
        type: KeepassImplementation,
        fsResolver: FileSystemResolver,
        fsOptions: FSOptions,
        file: FileDescriptor,
        input: OperationResult<InputStream>,
        key: EncryptedDatabaseKey
    ): OperationResult<EncryptedDatabase> {
        val openResult = when (type) {
            KeepassImplementation.KOTPASS -> KotpassDatabase.open(
                fsResolver,
                fsOptions,
                file,
                input,
                key
            )
        }

        if (openResult.isFailed) {
            return openResult.takeError()
        }

        val db = openResult.getOrThrow()
            .apply {
                this.watcher.subscribe(syncStatusProvider)
            }

        return openResult.takeStatusWith(db)
    }

    private data class DatabaseReference(
        val type: KeepassImplementation,
        val database: EncryptedDatabase
    )
}