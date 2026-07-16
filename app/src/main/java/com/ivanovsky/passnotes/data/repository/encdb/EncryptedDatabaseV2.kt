package com.ivanovsky.passnotes.data.repository.encdb

import arrow.core.Either
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.repository.TemplateDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import java.util.concurrent.locks.ReentrantLock

/**
 * New version of [EncryptedDatabase],
 * without [com.ivanovsky.passnotes.data.entity.OperationResult]
 */
interface EncryptedDatabaseV2 {

    val groupDao: GroupDao
    val noteDao: NoteDao
    val templateDao: TemplateDao
    val watcher: DatabaseWatcher<EncryptedDatabaseV2>
    val lock: ReentrantLock
    fun getFile(): FileDescriptor
    fun getKey(): EncryptedDatabaseKey
    fun getFSOptions(): FSOptions
    fun getConfig(): Either<OperationError, EncryptedDatabaseConfig>
    fun applyConfig(config: EncryptedDatabaseConfig): Either<OperationError, Boolean>
    fun commit(): Either<OperationError, Boolean>
    fun commitTo(output: FileDescriptor, fsOptions: FSOptions): Either<OperationError, Boolean>

    // TODO: refactor, change key should not invoke commit
    fun changeKey(
        oldKey: EncryptedDatabaseKey,
        newKey: EncryptedDatabaseKey
    ): Either<OperationError, Boolean>
}