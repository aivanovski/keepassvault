package com.ivanovsky.passnotes.data.repository.encdb

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.TemplateDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.util.toOperationResult
import java.util.concurrent.locks.ReentrantLock

/**
 * Adapts [EncryptedDatabaseV2] to the old [EncryptedDatabase] interface.
 */
class EncryptedDatabaseAdapter(
    private val db: EncryptedDatabaseV2
) : EncryptedDatabase, DatabaseWatcher.OnCommitListener<EncryptedDatabaseV2> {

    private val watcher = DatabaseWatcher<EncryptedDatabase>()

    init {
        db.watcher.subscribe(this)
    }

    override fun getGroupDao(): GroupDao = db.groupDao
    override fun getNoteDao(): NoteDao = db.noteDao
    override fun getTemplateDao(): TemplateDao = db.templateDao
    override fun getWatcher(): DatabaseWatcher<EncryptedDatabase> = watcher
    override fun getLock(): ReentrantLock = db.lock
    override fun getFile(): FileDescriptor = db.getFile()
    override fun getKey(): EncryptedDatabaseKey = db.getKey()
    override fun getFSOptions(): FSOptions = db.getFSOptions()

    override fun getConfig(): OperationResult<EncryptedDatabaseConfig> =
        db.getConfig().toOperationResult()

    override fun applyConfig(config: EncryptedDatabaseConfig): OperationResult<Boolean> =
        db.applyConfig(config).toOperationResult()

    override fun changeKey(
        oldKey: EncryptedDatabaseKey,
        newKey: EncryptedDatabaseKey
    ): OperationResult<Boolean> =
        db.changeKey(oldKey = oldKey, newKey = newKey).toOperationResult()

    override fun commit(): OperationResult<Boolean> = db.commit().toOperationResult()

    override fun commitTo(
        output: FileDescriptor,
        fsOptions: FSOptions
    ): OperationResult<Boolean> =
        db.commitTo(output, fsOptions).toOperationResult()

    override fun onCommit(
        database: EncryptedDatabaseV2,
        result: OperationResult<*>
    ) {
        watcher.notifyOnCommit(this, result)
    }
}