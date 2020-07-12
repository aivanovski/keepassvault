package com.ivanovsky.passnotes.domain.interactor.notes

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import java.util.*

class NotesInteractor(dbRepo: EncryptedDatabaseRepository) {

    private val noteRepository = dbRepo.noteRepository

    fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> {
        return noteRepository.getNotesByGroupUid(groupUid)
    }
}