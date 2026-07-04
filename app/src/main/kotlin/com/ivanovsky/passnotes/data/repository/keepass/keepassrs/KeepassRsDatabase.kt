package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import app.keemobile.kotpass.models.Group
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.raise.either
import com.google.protobuf.ByteString
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_DECODE_DB_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_OPEN_DB_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.newAuthError
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.DatabaseWatcher
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseAdapter
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseV2
import com.ivanovsky.passnotes.data.repository.encdb.MutableEncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.FileKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.TemplateDaoImpl
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.model.InheritableOptions
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Database as RawDatabase
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.DatabaseKey as RawKey
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Entry as RawEntry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Group as RawGroup
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.keepassrs.InvalidKeyException
import com.ivanovsky.passnotes.keepassrs.KeepassRs
import com.ivanovsky.passnotes.keepassrs.KeepassRsException
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.toOperationResult
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import timber.log.Timber

class KeepassRsDatabase(
    private val fsResolver: FileSystemResolver,
    private val fsOptions: FSOptions,
    file: FileDescriptor,
    key: EncryptedDatabaseKey,
    protoDatabase: RawDatabase
) : EncryptedDatabaseV2 {

    private val fileRef = AtomicReference(file)
    private val keyRef = AtomicReference(key)
    private val databaseRef = AtomicReference(protoDatabase)
    private val inheritableOptionsMap = AtomicReference(createInheritableOptionsMap())
    private val groupUidToParentMap = AtomicReference(createGroupUidToParentMap())

    override val lock = ReentrantLock()
    override val groupDao = KeepassRsGroupDao(this)
    override val noteDao = KeepassRsNoteDao(this)
    override val watcher = DatabaseWatcher<EncryptedDatabaseV2>()
    override val templateDao = TemplateDaoImpl(groupDao, noteDao)

    override fun getFile(): FileDescriptor = fileRef.get()

    override fun getKey(): EncryptedDatabaseKey = keyRef.get()

    override fun getFSOptions(): FSOptions = fsOptions

    override fun getConfig(): Either<OperationError, EncryptedDatabaseConfig> {
        return lock.withLock {
            val meta = databaseRef.get().meta
            Either.Right(
                MutableEncryptedDatabaseConfig(
                    isRecycleBinEnabled = meta.hasRecycleBinEnabled() && meta.recycleBinEnabled,
                    recycleBinUid = meta.recycleBinUuid.toUuidOrNull(),
                    maxHistoryItems = if (meta.hasHistoryMaxItems()) meta.historyMaxItems else 0
                )
            )
        }
    }

    override fun applyConfig(config: EncryptedDatabaseConfig): Either<OperationError, Boolean> {
        return lock.withLock {
            val updatedMeta = databaseRef.get().meta.toBuilder()
                .setRecycleBinEnabled(config.isRecycleBinEnabled)
                .setHistoryMaxItems(config.maxHistoryItems)
                .build()

            swapDatabase { db ->
                db.toBuilder()
                    .setMeta(updatedMeta)
                    .build()
            }

            commit()
        }
    }

    override fun changeKey(
        oldKey: EncryptedDatabaseKey,
        newKey: EncryptedDatabaseKey
    ): Either<OperationError, Boolean> {
        return lock.withLock {
            either {
                if (oldKey != keyRef.get()) {
                    raise(
                        newAuthError(
                            OperationError.MESSAGE_INVALID_PASSWORD,
                            Stacktrace()
                        )
                    )
                }

                keyRef.set(newKey)

                commit().bind()
            }
        }
    }

    override fun commit(): Either<OperationError, Boolean> {
        val updatedFile = fileRef.get().copy(modified = System.currentTimeMillis())
        val result = commitTo(updatedFile, fsOptions)
        if (result.isRight()) {
            fileRef.set(updatedFile)
        }

        return result
    }

    override fun commitTo(
        output: FileDescriptor,
        fsOptions: FSOptions
    ): Either<OperationError, Boolean> {
        val fsProvider = fsResolver.resolveProvider(output.fsAuthority)

        val commitResult = lock.withLock {
            either {
                val key = createKey(fsResolver, keyRef.get()).toEither().bind()

                val databaseBytes = KeepassRs.encode(
                    database = databaseRef.get(),
                    key = key
                )
                    .mapLeft { error ->
                        newDbError(OperationError.MESSAGE_FAILED_TO_ENCODE_DATA, error)
                    }
                    .bind()

                val output = fsProvider.openFileForWrite(
                    output,
                    OnConflictStrategy.CANCEL,
                    fsOptions
                ).toEither().bind()

                InputOutputUtils.copy(
                    from = ByteArrayInputStream(databaseBytes),
                    to = output,
                    isClose = true
                )
                    .takeStatusWith(true)
                    .toEither().bind()
            }
        }

        if (commitResult.isRight()) {
            watcher.notifyOnCommit(this, commitResult.toOperationResult())
        }

        return commitResult
    }

    fun getRawDatabase(): RawDatabase = databaseRef.get()

    fun swapDatabase(transform: (source: RawDatabase) -> RawDatabase) {
        lock.withLock {
            databaseRef.set(transform.invoke(databaseRef.get()))
            inheritableOptionsMap.set(createInheritableOptionsMap())
            groupUidToParentMap.set(createGroupUidToParentMap())
        }
    }

    fun getRawGroupByUid(uid: UUID): Either<OperationError, RawGroup> =
        either {
            val group = getRawDatabase()
                .rootGroup
                .getGroup(
                    predicate = { group -> group.uuid.toUuidOrNull() == uid }
                ) ?: raise(failedToFindGroupByUid(uid))

            group
        }

    fun getRawEntryByUid(uid: UUID): Either<OperationError, RawEntry> =
        either {
            val (_, entry) = getRawDatabase()
                .rootGroup
                .getEntryAndGroup { entry -> entry.uuid.toUuidOrNull() == uid }
                ?: raise(failedToFindEntryByUid(uid))

            entry
        }

    fun getRawEntryWithGroupByUid(uid: UUID): Either<OperationError, Pair<RawGroup, RawEntry>> =
        either {
            val (group, entry) = getRawDatabase()
                .rootGroup
                .getEntryAndGroup { entry -> entry.uuid.toUuidOrNull() == uid }
                ?: raise(failedToFindEntryByUid(uid))

            group to entry
        }

    fun failedToFindGroupByUid(uid: UUID): OperationError =
        newDbError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                Group::class.simpleName,
                uid
            ),
            Stacktrace()
        )

    fun failedToFindEntryByUid(uid: UUID): OperationError =
        newDbError(
            String.format(
                GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                Note::class.simpleName,
                uid
            ),
            Stacktrace()
        )

    fun getAllGroups(): List<RawGroup> {
        return getRawDatabase().rootGroup
            .collectEntries { group, _ -> listOf(group) }
    }

    fun getInheritableOptions(groupUid: UUID): Either<OperationError, InheritableOptions> {
        val options = inheritableOptionsMap.get()[groupUid]
            ?: return Either.Left(failedToFindGroupByUid(groupUid))

        return Either.Right(options)
    }

    fun getRawParentGroup(childUid: UUID): Either<OperationError, Option<RawGroup>> =
        either {
            val rootUid = getRawDatabase().rootGroup.uuid.toUuid().bind()
            if (childUid != rootUid) {
                val parentGroup = groupUidToParentMap.get()[childUid]
                    ?: raise(failedToFindGroupByUid(childUid))

                Some(parentGroup)
            } else {
                None
            }
        }

    fun getParentGroupUid(childUid: UUID): Either<OperationError, Option<UUID>> =
        either {
            val parentGroup = getRawParentGroup(childUid).bind()

            val parentUid = parentGroup.flatMap { group -> group.uuid.toUuid().getOrNone() }

            parentUid
        }

    private fun getRawRootGroupOptions(): InheritableOptions {
        val root = getRawDatabase().rootGroup

        return root.getInheritableOptions(
            parentOptions = InheritableOptions(
                autotypeEnabled = InheritableBooleanOption.ENABLED,
                searchEnabled = InheritableBooleanOption.ENABLED
            )
        )
    }

    private fun RawGroup.getInheritableOptions(
        parentOptions: InheritableOptions
    ): InheritableOptions {
        return InheritableOptions(
            autotypeEnabled = if (hasEnableAutotype()) {
                InheritableBooleanOption(
                    isEnabled = enableAutotype,
                    isInheritValue = false
                )
            } else {
                InheritableBooleanOption(
                    isEnabled = parentOptions.autotypeEnabled.isEnabled,
                    isInheritValue = true
                )
            },
            searchEnabled = if (hasEnableSearching()) {
                InheritableBooleanOption(
                    isEnabled = enableSearching,
                    isInheritValue = false
                )
            } else {
                InheritableBooleanOption(
                    isEnabled = parentOptions.searchEnabled.isEnabled,
                    isInheritValue = true
                )
            }
        )
    }

    private fun createInheritableOptionsMap(): Map<UUID, InheritableOptions> {
        val result = mutableListOf<Pair<ByteString, InheritableOptions>>()

        val root = getRawDatabase().rootGroup
        val rootOptions = getRawRootGroupOptions()
        result.add(root.uuid to rootOptions)

        val nextGroups = LinkedList<Pair<RawGroup, InheritableOptions>>()
            .apply {
                add(Pair(root, rootOptions))
            }

        while (nextGroups.isNotEmpty()) {
            val (group, parentOptions) = nextGroups.removeFirst()

            val options = group.getInheritableOptions(parentOptions)
            result.add(group.uuid to options)

            for (child in group.groupsList) {
                nextGroups.add(Pair(child, options))
            }
        }

        return result
            .mapNotNull { (key, options) ->
                val uuid = key.toUuidOrNull() ?: return@mapNotNull null

                uuid to options
            }
            .toMap()
    }

    private fun createGroupUidToParentMap(): Map<UUID, RawGroup> {
        val result = hashMapOf<UUID, RawGroup>()

        val nextGroups = LinkedList<RawGroup>()
            .apply {
                add(getRawDatabase().rootGroup)
            }

        while (nextGroups.isNotEmpty()) {
            val group = nextGroups.removeFirst()

            for (child in group.groupsList) {
                val childUid = child.uuid.toUuidOrNull() ?: continue
                result[childUid] = group
                nextGroups.add(child)
            }
        }

        return result
    }

    companion object {

        fun open(
            fsResolver: FileSystemResolver,
            fsOptions: FSOptions,
            file: FileDescriptor,
            content: OperationResult<InputStream>,
            key: EncryptedDatabaseKey
        ): Either<OperationError, EncryptedDatabase> =
            either {
                val databaseBytes = InputOutputUtils
                    .readAllBytes(
                        source = content.toEither().bind(),
                        isCloseOnFinish = true
                    )
                    .mapLeft { error ->
                        newDbError(MESSAGE_FAILED_TO_OPEN_DB_FILE, error.throwable as? Exception)
                    }
                    .bind()

                val database = KeepassRs
                    .decode(
                        data = databaseBytes,
                        key = createKey(fsResolver, key).toEither().bind()
                    )
                    .mapLeft { error -> error.toOperationError() }
                    .bind()

                EncryptedDatabaseAdapter(
                    db = KeepassRsDatabase(
                        fsResolver = fsResolver,
                        fsOptions = fsOptions,
                        file = file,
                        key = key,
                        protoDatabase = database
                    )
                )
            }

        private fun KeepassRsException.toOperationError(): OperationError {
            return when (this) {
                is InvalidKeyException -> newAuthError(message.orEmpty(), Stacktrace())
                else -> newDbError(
                    if (message.isNullOrEmpty()) {
                        MESSAGE_FAILED_TO_DECODE_DB_FILE
                    } else {
                        message.orEmpty()
                    },
                    Stacktrace()
                )
            }
        }

        private fun invalidKeyError(key: EncryptedDatabaseKey): OperationError {
            return newAuthError(
                if (key.type == KeyType.PASSWORD) {
                    OperationError.MESSAGE_INVALID_PASSWORD
                } else {
                    OperationError.MESSAGE_INVALID_KEY_FILE
                },
                Stacktrace()
            )
        }

        private fun createKey(
            fsResolver: FileSystemResolver,
            key: EncryptedDatabaseKey
        ): OperationResult<RawKey> {
            return when (key) {
                is PasswordKeepassKey -> {
                    OperationResult.success(
                        RawKey.newBuilder()
                            .setPassword(key.password)
                            .build()
                    )
                }

                is FileKeepassKey -> {
                    val fsProvider = fsResolver.resolveProvider(key.file.fsAuthority)
                    val inputResult = fsProvider.openFileForRead(
                        key.file,
                        OnConflictStrategy.CANCEL,
                        FSOptions.READ_ONLY
                    )
                    if (inputResult.isFailed) {
                        return inputResult.takeError()
                    }

                    try {
                        val keyBytes = inputResult.obj.use { input ->
                            input.readBytes()
                        }
                        val builder = RawKey.newBuilder()
                            .setKeyBytes(ByteString.copyFrom(keyBytes))

                        key.password?.let { password ->
                            builder.setPassword(password)
                        }

                        OperationResult.success(builder.build())
                    } catch (exception: Exception) {
                        Timber.d(exception)
                        OperationResult.error(
                            OperationError.newGenericIOError(
                                OperationError.MESSAGE_FAILED_TO_READ_KEY_FILE,
                                exception
                            )
                        )
                    }
                }

                else -> {
                    OperationResult.error(
                        invalidKeyError(key)
                    )
                }
            }
        }
    }
}