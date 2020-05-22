package com.ivanovsky.passnotes.domain.interactor.notes

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.NoteRepository
import java.util.*

class NotesInteractor(private val noteRepository: NoteRepository) {

    fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> {
        return noteRepository.getNotesByGroupUid(groupUid)
    }
}