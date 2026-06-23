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
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.TemplateDaoImpl
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.ReadDatabaseResponse
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.databaseOrNull
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.rust.RustBridge
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.util.InputOutputUtils
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import timber.log.Timber
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Database as ProtoDatabase

class KeepassRsDatabase(
    private val fsOptions: FSOptions,
    private val file: FileDescriptor,
    private val key: EncryptedDatabaseKey,
    private val protoDatabase: ProtoDatabase
) : EncryptedDatabase {

    private val lock = ReentrantLock()
    private val groupDao = KeepassRsGroupDao(this)
    private val noteDao = KeepassRsNoteDao(this)
    private val templateDao = TemplateDaoImpl(groupDao, noteDao)
    private val dbWatcher = DatabaseWatcher()

    override fun getLock(): ReentrantLock = lock

    override fun getFile(): FileDescriptor = file

    override fun getKey(): EncryptedDatabaseKey = key

    override fun getFSOptions(): FSOptions = fsOptions

    override fun getConfig(): OperationResult<EncryptedDatabaseConfig> {
        return lock.withLock {
            val meta = protoDatabase.meta
            OperationResult.success(
                MutableEncryptedDatabaseConfig(
                    isRecycleBinEnabled = meta.hasRecycleBinEnabled() && meta.recycleBinEnabled,
                    recycleBinUid = meta.recycleBinUuid.toUuidOrNull(),
                    maxHistoryItems = if (meta.hasHistoryMaxItems()) meta.historyMaxItems else 0
                )
            )
        }
    }

    override fun applyConfig(config: EncryptedDatabaseConfig): OperationResult<Boolean> =
        unsupportedWriteOperation()

    override fun getGroupDao(): GroupDao = groupDao

    override fun getNoteDao(): NoteDao = noteDao

    override fun getTemplateDao(): TemplateDao = templateDao

    override fun changeKey(
        oldKey: EncryptedDatabaseKey,
        newKey: EncryptedDatabaseKey
    ): OperationResult<Boolean> = unsupportedWriteOperation()

    override fun commit(): OperationResult<Boolean> = unsupportedWriteOperation()

    override fun commitTo(
        output: FileDescriptor,
        fsOptions: FSOptions
    ): OperationResult<Boolean> = unsupportedWriteOperation()

    override fun getWatcher(): DatabaseWatcher = dbWatcher

    fun getRawDatabase(): ProtoDatabase = protoDatabase

    companion object {

        fun open(
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
                        fsOptions = fsOptions,
                        file = file,
                        key = key,
                        protoDatabase = database
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
