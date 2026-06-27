package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.newAuthError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.TemplateDao
import com.ivanovsky.passnotes.data.repository.encdb.DatabaseWatcher
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.encdb.MutableEncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.TemplateDaoImpl
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.ReadDatabaseResponse
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.writeDatabaseRequest
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.databaseOrNull
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.rust.RustBridge
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.util.InputOutputUtils
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import timber.log.Timber
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Database as ProtoDatabase

class KeepassRsDatabase(
    private val fsResolver: FileSystemResolver,
    private val fsOptions: FSOptions,
    file: FileDescriptor,
    key: EncryptedDatabaseKey,
    protoDatabase: ProtoDatabase,
    originalData: ByteArray
) : EncryptedDatabase {

    private val lock = ReentrantLock()
    private val fileRef = AtomicReference(file)
    private val keyRef = AtomicReference(key)
    private val databaseRef = AtomicReference(protoDatabase)
    private val originalDataRef = AtomicReference(originalData)
    private val groupDao = KeepassRsGroupDao(this)
    private val noteDao = KeepassRsNoteDao(this)
    private val templateDao = TemplateDaoImpl(groupDao, noteDao)
    private val dbWatcher = DatabaseWatcher()

    override fun getLock(): ReentrantLock = lock

    override fun getFile(): FileDescriptor = fileRef.get()

    override fun getKey(): EncryptedDatabaseKey = keyRef.get()

    override fun getFSOptions(): FSOptions = fsOptions

    override fun getConfig(): OperationResult<EncryptedDatabaseConfig> {
        return lock.withLock {
            val meta = databaseRef.get().meta
            OperationResult.success(
                MutableEncryptedDatabaseConfig(
                    isRecycleBinEnabled = meta.hasRecycleBinEnabled() && meta.recycleBinEnabled,
                    recycleBinUid = meta.recycleBinUuid.toUuidOrNull(),
                    maxHistoryItems = if (meta.hasHistoryMaxItems()) meta.historyMaxItems else 0
                )
            )
        }
    }

    override fun applyConfig(config: EncryptedDatabaseConfig): OperationResult<Boolean> {
        return lock.withLock {
            val updatedMeta = databaseRef.get().meta.toBuilder()
                .setRecycleBinEnabled(config.isRecycleBinEnabled)
                .setHistoryMaxItems(config.maxHistoryItems)
                .build()
            updateDatabase { database ->
                database.toBuilder()
                    .setMeta(updatedMeta)
                    .build()
            }

            commit()
        }
    }

    override fun getGroupDao(): GroupDao = groupDao

    override fun getNoteDao(): NoteDao = noteDao

    override fun getTemplateDao(): TemplateDao = templateDao

    override fun changeKey(
        oldKey: EncryptedDatabaseKey,
        newKey: EncryptedDatabaseKey
    ): OperationResult<Boolean> {
        return lock.withLock {
            if (oldKey != keyRef.get()) {
                return@withLock OperationResult.error(
                    newAuthError(
                        OperationError.MESSAGE_INVALID_PASSWORD,
                        Stacktrace()
                    )
                )
            }

            if (oldKey !is PasswordKeepassKey || newKey !is PasswordKeepassKey) {
                return@withLock unsupportedWriteOperation()
            }

            val updatedFile = fileRef.get().copy(modified = System.currentTimeMillis())
            val result = commitWithPassword(
                output = updatedFile,
                fsOptions = fsOptions,
                oldPassword = oldKey.password,
                newPassword = newKey.password
            )
            if (result.isSucceededOrDeferred) {
                keyRef.set(newKey)
                fileRef.set(updatedFile)
            }

            result
        }
    }

    override fun commit(): OperationResult<Boolean> {
        val updatedFile = fileRef.get().copy(modified = System.currentTimeMillis())
        val result = commitTo(updatedFile, fsOptions)
        if (result.isSucceededOrDeferred) {
            fileRef.set(updatedFile)
        }

        return result
    }

    override fun commitTo(
        output: FileDescriptor,
        fsOptions: FSOptions
    ): OperationResult<Boolean> {
        val currentKey = keyRef.get()
        if (currentKey !is PasswordKeepassKey) {
            return OperationResult.error(
                newAuthError(OperationError.MESSAGE_INVALID_PASSWORD, Stacktrace())
            )
        }

        return commitWithPassword(
            output = output,
            fsOptions = fsOptions,
            oldPassword = currentKey.password,
            newPassword = currentKey.password
        )
    }

    private fun commitWithPassword(
        output: FileDescriptor,
        fsOptions: FSOptions,
        oldPassword: String,
        newPassword: String
    ): OperationResult<Boolean> {
        val request = writeDatabaseRequest {
            database = databaseRef.get()
        }
        val encoded = RustBridge.nativeWriteDatabaseWithPasswords(
            originalDatabaseData = originalDataRef.get(),
            writeRequestData = request.toByteArray(),
            oldPassword = oldPassword,
            newPassword = newPassword
        ) ?: return OperationResult.error(
            OperationError.newDbError(
                "Failed to write DB file",
                Stacktrace()
            )
        )

        val fsProvider = fsResolver.resolveProvider(output.fsAuthority)
        val outResult = fsProvider.openFileForWrite(
            output,
            OnConflictStrategy.CANCEL,
            fsOptions
        )
        if (outResult.isFailed) {
            return outResult.mapError()
        }

        val out = outResult.obj
        val result = try {
            out.write(encoded)
            outResult.takeStatusWith(true)
        } catch (exception: IOException) {
            OperationResult.error(OperationError.newGenericIOError(exception))
        } finally {
            InputOutputUtils.close(out)
        }

        if (result.isSucceededOrDeferred) {
            originalDataRef.set(encoded)
            dbWatcher.notifyOnCommit(this, result)
        }

        return result
    }

    override fun getWatcher(): DatabaseWatcher = dbWatcher

    fun getRawDatabase(): ProtoDatabase = databaseRef.get()

    fun updateDatabase(transform: (ProtoDatabase) -> ProtoDatabase) {
        databaseRef.set(transform(databaseRef.get()))
    }

    companion object {

        fun open(
            fsResolver: FileSystemResolver,
            fsOptions: FSOptions,
            file: FileDescriptor,
            content: OperationResult<InputStream>,
            key: EncryptedDatabaseKey
        ): OperationResult<KeepassRsDatabase> {
            if (content.isFailed) {
                return content.mapError()
            }

            if (key !is PasswordKeepassKey) {
                return OperationResult.error(
                    newAuthError(
                        if (key.type == KeyType.PASSWORD) {
                            OperationError.MESSAGE_INVALID_PASSWORD
                        } else {
                            OperationError.MESSAGE_INVALID_KEY_FILE
                        },
                        Stacktrace()
                    )
                )
            }

            val contentStream = content.obj
            try {
                val databaseData = contentStream.readBytes()
                val responseBytes = RustBridge.nativeCanDecodeWithPassword(
                    databaseData = databaseData,
                    password = key.password
                ) ?: return OperationResult.error(
                    newAuthError(
                        OperationError.MESSAGE_INVALID_PASSWORD,
                        Stacktrace()
                    )
                )

                val database = ReadDatabaseResponse.parseFrom(responseBytes).databaseOrNull
                    ?: return OperationResult.error(
                        OperationError.newDbError(
                            OperationError.MESSAGE_FAILED_TO_OPEN_DB_FILE,
                            Stacktrace()
                        )
                    )

                if (!database.hasRootGroup()) {
                    return OperationResult.error(
                        OperationError.newDbError(
                            OperationError.MESSAGE_FAILED_TO_FIND_ROOT_GROUP,
                            Stacktrace()
                        )
                    )
                }

                return OperationResult.success(
                    KeepassRsDatabase(
                        fsResolver = fsResolver,
                        fsOptions = fsOptions,
                        file = file,
                        key = key,
                        protoDatabase = database,
                        originalData = databaseData
                    )
                )
            } catch (exception: Exception) {
                Timber.d(exception)

                val message = exception.message
                    ?: OperationError.MESSAGE_FAILED_TO_OPEN_DB_FILE

                return OperationResult.error(
                    OperationError.newDbError(message, exception)
                )
            } finally {
                InputOutputUtils.close(contentStream)
            }
        }
    }
}

internal fun <T> unsupportedWriteOperation(): OperationResult<T> =
    OperationResult.error(
        OperationError.newDbError(
            OperationError.MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED,
            Stacktrace()
        )
    )
