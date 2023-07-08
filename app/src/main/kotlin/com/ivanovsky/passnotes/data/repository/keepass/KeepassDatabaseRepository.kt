package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FSOptions.Companion.defaultOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.keepass_java.OnStatusChangeListener
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class KeepassDatabaseRepository(
    private val fileSystemResolver: FileSystemResolver,
    private val lockInteractor: DatabaseLockInteractor,
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

            val statusListener = { status: DatabaseStatus ->
                observerBus.notifyDatabaseStatusChanged(status)
            }

            val openResult = openDatabase(
                type,
                fsProvider,
                options,
                file,
                openFileResult,
                key,
                statusListener
            )
            if (openResult.isFailed) {
                return@withLock openResult.takeError()
            }

            val db = openResult.obj
            database.set(DatabaseReference(type, db))
            openResult.takeStatusWith(db)
        }

        if (openDbResult.isSucceededOrDeferred) {
            onDatabaseOpened(options, openDbResult.obj.status)
        }

        return openDbResult
    }

    override fun createNew(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor,
        addTemplates: Boolean
    ): OperationResult<Boolean> {
        val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

        return lock.withLock {
            val dbResult = KotpassDatabase.new(
                fsProvider = fsProvider,
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
                database.set(null)
            }
        }

        onDatabaseClosed()

        return OperationResult.success(true)
    }

    private fun onDatabaseOpened(fsOptions: FSOptions, status: DatabaseStatus) {
        lockInteractor.onDatabaseOpened(fsOptions, status)
        observerBus.notifyDatabaseOpened(fsOptions, status)
    }

    private fun onDatabaseClosed() {
        lockInteractor.onDatabaseClosed()
        observerBus.notifyDatabaseClosed()
    }

    private fun openDatabase(
        type: KeepassImplementation,
        fsProvider: FileSystemProvider,
        fsOptions: FSOptions,
        file: FileDescriptor,
        input: OperationResult<InputStream>,
        key: EncryptedDatabaseKey,
        statusListener: OnStatusChangeListener?
    ): OperationResult<EncryptedDatabase> {
        val openResult = when (type) {
            KeepassImplementation.KOTPASS -> KotpassDatabase.open(
                fsProvider,
                fsOptions,
                file,
                input,
                key,
                statusListener
            )
        }
        if (openResult.isFailed) {
            return openResult.takeError()
        }

        return openResult.takeStatusWith(openResult.obj)
    }

    private data class DatabaseReference(
        val type: KeepassImplementation,
        val database: EncryptedDatabase
    )
}