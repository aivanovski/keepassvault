package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNSUPPORTED_OPERATION
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.TemplateDao
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.encdb.MutableEncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.FileKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.keepass_java.KeepassJavaDatabase
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.TemplateDaoImpl
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.util.InputOutputUtils
import io.github.anvell.kotpass.database.Credentials
import io.github.anvell.kotpass.database.KeePassDatabase
import io.github.anvell.kotpass.database.decode
import io.github.anvell.kotpass.database.encode
import io.github.anvell.kotpass.database.modifiers.modifyMeta
import io.github.anvell.kotpass.models.Entry
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import io.github.anvell.kotpass.models.Group as RawGroup

class KotpassDatabase(
    private val fsProvider: FileSystemProvider,
    private val fsOptions: FSOptions,
    private val file: FileDescriptor,
    private val key: EncryptedDatabaseKey,
    db: KeePassDatabase,
    private val statusListener: KeepassJavaDatabase.OnStatusChangeListener?,
    status: DatabaseStatus
) : EncryptedDatabase {

    private val lock = ReentrantLock()
    private val database = AtomicReference(db)
    private val status = AtomicReference(status)
    private val groupDao = KotpassGroupDao(this)
    private val noteDao = KotpassNoteDao(this)
    private val templateDao = TemplateDaoImpl(groupDao, noteDao)

    override fun getLock(): ReentrantLock = lock

    override fun getFile(): FileDescriptor = file

    override fun getStatus(): DatabaseStatus = status.get()

    override fun getGroupDao(): GroupDao = groupDao

    override fun getNoteDao(): NoteDao = noteDao

    override fun getTemplateDao(): TemplateDao = templateDao

    override fun getConfig(): OperationResult<EncryptedDatabaseConfig> {
        return lock.withLock {
            val rawDatabase = getRawDatabase()

            val config = MutableEncryptedDatabaseConfig(
                isRecycleBinEnabled = rawDatabase.content.meta.recycleBinEnabled
            )

            OperationResult.success(config)
        }
    }

    override fun applyConfig(newConfig: EncryptedDatabaseConfig): OperationResult<Boolean> {
        return lock.withLock {
            val getOldConfigResult = config
            if (getOldConfigResult.isFailed) {
                return@withLock getOldConfigResult.mapError()
            }

            val oldConfig = getOldConfigResult.obj

            if (oldConfig != newConfig) {
                swapDatabase(
                    getRawDatabase().modifyMeta {
                        copy(
                            recycleBinEnabled = newConfig.isRecycleBinEnabled
                        )
                    }
                )
            }

            commit()
        }
    }

    override fun changeKey(
        oldKey: EncryptedDatabaseKey,
        newKey: EncryptedDatabaseKey
    ): OperationResult<Boolean> {
        return OperationResult.error(newDbError(MESSAGE_UNSUPPORTED_OPERATION))
    }

    override fun commit(): OperationResult<Boolean> {
        return lock.withLock {
            val updatedFile = file.copy(modified = System.currentTimeMillis())

            val outResult =
                fsProvider.openFileForWrite(updatedFile, OnConflictStrategy.CANCEL, fsOptions)
            if (outResult.isFailed) {
                return outResult.mapError()
            }

            val out = outResult.obj
            val db = database.get()
            try {
                db.encode(out)

                val result = outResult.takeStatusWith(true)
                val newStatus = determineDatabaseStatus(fsOptions, result)
                if (status.get() != newStatus) {
                    status.set(newStatus)
                    statusListener?.onDatabaseStatusChanged(newStatus)
                }

                result
            } catch (e: IOException) {
                InputOutputUtils.close(out)

                OperationResult.error(newGenericIOError(e))
            }
        }
    }

    fun swapDatabase(db: KeePassDatabase) {
        lock.withLock {
            database.set(db)
        }
    }

    fun getRawDatabase(): KeePassDatabase = database.get()

    fun getRawRootGroup(): RawGroup = database.get().content.group

    fun getRawParentGroup(childUid: UUID): OperationResult<RawGroup> {
        val rootGroup = database.get().content.group

        val (_, parentGroup) = rootGroup.findChildGroup { parentGroup ->
            parentGroup.groups.any { it.uuid == childUid }
        }
            ?: return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP))

        return OperationResult.success(parentGroup)
    }

    fun getRawGroupByUid(uid: UUID): OperationResult<RawGroup> {
        val rootGroup = database.get().content.group
        if (rootGroup.uuid == uid) {
            return OperationResult.success(rootGroup)
        }

        val (_, parentGroup) = rootGroup.findChildGroup { it.uuid == uid }
            ?: return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP))

        return OperationResult.success(parentGroup)
    }

    fun getRawChildGroups(root: RawGroup): OperationResult<List<RawGroup>> {
        val nextGroups = LinkedList<RawGroup>()
            .apply {
                add(root)
            }

        val allGroups = mutableListOf<RawGroup>()

        while (nextGroups.size > 0) {
            val currentGroup = nextGroups.pop()

            nextGroups.addAll(currentGroup.groups)

            allGroups.add(currentGroup)
            allGroups.addAll(currentGroup.groups)
        }

        return OperationResult.success(allGroups)
    }

    fun getRawChildEntries(root: RawGroup): List<Entry> {
        val nextGroups = LinkedList<RawGroup>()
            .apply {
                add(root)
            }

        val allEntries = mutableListOf<Entry>()

        while (nextGroups.size > 0) {
            val currentGroup = nextGroups.pop()
            nextGroups.addAll(currentGroup.groups)
            allEntries.addAll(currentGroup.entries)
        }

        return allEntries
    }

    fun <T> collectEntries(
        root: RawGroup,
        transform: (group: RawGroup, groupEntries: List<Entry>) -> List<T>
    ): List<T> {
        val result = mutableListOf<T>()

        val nextGroups = LinkedList<RawGroup>()
            .apply {
                add(root)
            }

        while (nextGroups.size > 0) {
            val currentGroup = nextGroups.pop()
            nextGroups.addAll(currentGroup.groups)
            result.addAll(transform.invoke(currentGroup, currentGroup.entries))
        }

        return result
    }

    companion object {

        fun open(
            fsProvider: FileSystemProvider,
            fsOptions: FSOptions,
            file: FileDescriptor,
            content: OperationResult<InputStream>,
            key: EncryptedDatabaseKey,
            statusListener: KeepassJavaDatabase.OnStatusChangeListener?
        ): OperationResult<KotpassDatabase> {
            if (content.isFailed) {
                return content.mapError()
            }

            val contentStream = content.obj

            try {
                val db = KeePassDatabase.decode(contentStream, key.toCredentials())

                return OperationResult.success(
                    KotpassDatabase(
                        fsProvider = fsProvider,
                        fsOptions = fsOptions,
                        file = file,
                        key = key,
                        db = db,
                        statusListener = statusListener,
                        status = determineDatabaseStatus(fsOptions, content)
                    )
                )
            } catch (e: Exception) {
                Timber.d(e)

                val message = if (!e.message.isNullOrEmpty()) {
                    e.message
                } else {
                    OperationError.MESSAGE_FAILED_TO_OPEN_DB_FILE
                }

                return if (e is IOException) {
                    OperationResult.error(newGenericIOError(message, e))
                } else {
                    OperationResult.error(newDbError(message, e))
                }
            } finally {
                InputOutputUtils.close(contentStream)
            }
        }

        private fun determineDatabaseStatus(
            fsOptions: FSOptions,
            lastOperation: OperationResult<*>
        ): DatabaseStatus {
            return if (!fsOptions.isWriteEnabled) {
                DatabaseStatus.READ_ONLY
            } else if (lastOperation.isDeferred && !fsOptions.isPostponedSyncEnabled) {
                DatabaseStatus.CACHED
            } else if (lastOperation.isDeferred && fsOptions.isPostponedSyncEnabled) {
                DatabaseStatus.POSTPONED_CHANGES
            } else {
                DatabaseStatus.NORMAL
            }
        }

        private fun EncryptedDatabaseKey.toCredentials(): Credentials {
            return when (this) {
                is PasswordKeepassKey -> {
                    Credentials.from(this.getKey().obj)
                }
                is FileKeepassKey -> {
                    throw NotImplementedError()
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}