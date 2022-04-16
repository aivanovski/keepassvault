package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import java.util.UUID

class NoteRepositoryWrapper : RepositoryWrapperWithDatabase(), NoteRepository {

    override fun getNotesByGroupUid(groupUid: UUID): OperationResult<MutableList<Note>> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.noteRepository.getNotesByGroupUid(groupUid)
    }

    override fun insert(note: Note): OperationResult<UUID> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.noteRepository.insert(note)
    }

    override fun getNoteByUid(uid: UUID): OperationResult<Note> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.noteRepository.getNoteByUid(uid)
    }

    override fun update(note: Note): OperationResult<UUID> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.noteRepository.update(note)
    }

    override fun remove(noteUid: UUID): OperationResult<Boolean> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.noteRepository.remove(noteUid)
    }

    override fun find(query: String): OperationResult<MutableList<Note>> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.noteRepository.find(query)
    }
}