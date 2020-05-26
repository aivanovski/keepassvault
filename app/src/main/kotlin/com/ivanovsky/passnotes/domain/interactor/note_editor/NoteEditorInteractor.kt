package com.ivanovsky.passnotes.domain.interactor.note_editor

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.NoteRepository

class NoteEditorInteractor(
    private val noteRepository: NoteRepository,
    private val observerBus: ObserverBus
) {

    fun createNewNote(note: Note): OperationResult<Unit> {
        val insertResult = noteRepository.insert(note)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        observerBus.notifyNoteDataSetChanged(note.groupUid)

        return insertResult.takeStatusWith(Unit)
    }
}