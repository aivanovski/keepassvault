package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import java.util.UUID

class KeepassRsNoteDao(
    private val writableDao: NoteDao
) : NoteDao {

    override fun getAll(): OperationResult<List<Note>> = writableDao.all

    override fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> =
        writableDao.getNotesByGroupUid(groupUid)

    override fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        writableDao.getNoteByUid(noteUid)

    override fun insert(note: Note): OperationResult<UUID> = writableDao.insert(note)

    override fun insert(notes: List<Note>): OperationResult<Boolean> = writableDao.insert(notes)

    override fun insert(notes: List<Note>, doCommit: Boolean): OperationResult<Boolean> =
        writableDao.insert(notes, doCommit)

    override fun update(note: Note, doCommit: Boolean): OperationResult<UUID> =
        writableDao.update(note, doCommit)

    override fun remove(noteUid: UUID): OperationResult<Boolean> = writableDao.remove(noteUid)

    override fun find(query: String): OperationResult<List<Note>> = writableDao.find(query)

    override fun getContentWatcher(): ContentWatcher<Note> = writableDao.contentWatcher

    override fun getHistory(noteUid: UUID): OperationResult<List<Note>> =
        writableDao.getHistory(noteUid)
}
