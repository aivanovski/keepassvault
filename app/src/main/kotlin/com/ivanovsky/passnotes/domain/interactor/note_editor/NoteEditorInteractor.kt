package com.ivanovsky.passnotes.domain.interactor.note_editor

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import java.util.*

class NoteEditorInteractor(
    dbRepo: EncryptedDatabaseRepository,
    private val observerBus: ObserverBus
) {

    private val noteRepository = dbRepo.noteRepository

    fun createNewNote(note: Note): OperationResult<Unit> {
        val insertResult = noteRepository.insert(note)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        observerBus.notifyNoteDataSetChanged(note.groupUid)

        return insertResult.takeStatusWith(Unit)
    }

    fun loadNote(uid: UUID): OperationResult<Note> {
        return noteRepository.getNoteByUid(uid)
    }

    fun updateNote(note: Note): OperationResult<Unit> {
        val updateResult = noteRepository.update(note)
        if (updateResult.isFailed) {
            return updateResult.takeError()
        }

        val groupUid = note.groupUid
        val oldUid = note.uid
        val newUid = updateResult.obj

        observerBus.notifyNoteContentChanged(groupUid, oldUid, newUid)

        return updateResult.takeStatusWith(Unit)
    }
}