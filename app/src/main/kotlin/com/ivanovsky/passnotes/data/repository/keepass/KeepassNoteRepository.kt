package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.NoteRepository
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Property
import java.util.UUID

class KeepassNoteRepository(private val dao: NoteDao) : NoteRepository {

    override fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> {
        return dao.getNotesByGroupUid(groupUid)
    }

    override fun insert(note: Note): OperationResult<UUID> {
        return dao.insert(note)
    }

    override fun getNoteByUid(uid: UUID): OperationResult<Note> {
        return dao.getNoteByUid(uid)
    }

    override fun update(note: Note): OperationResult<UUID> {
        return dao.update(note)
    }

    override fun remove(noteUid: UUID): OperationResult<Boolean> {
        return dao.remove(noteUid)
    }

    override fun find(query: String): OperationResult<List<Note>> {
        val getNotesResult = dao.all
        if (getNotesResult.isFailed) {
            return getNotesResult.takeError()
        }

        val allNotes = getNotesResult.obj
        val matchedNotes = allNotes.filter { note -> note.matches(query) }

        return OperationResult.success(matchedNotes)
    }

    private fun Note.matches(query: String): Boolean {
        return title.contains(query, ignoreCase = true) ||
            properties.any { property -> property.matches(query) }
    }

    private fun Property.matches(query: String): Boolean {
        return name?.contains(query, ignoreCase = true) == true ||
            value?.contains(query, ignoreCase = true) == true
    }
}