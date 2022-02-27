package com.ivanovsky.passnotes.domain.interactor.note_editor

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteUseCase
import java.util.*

class NoteEditorInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val observerBus: ObserverBus,
) {

    fun createNewNote(note: Note): OperationResult<Unit> {
        val insertResult = dbRepo.noteRepository.insert(note)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        observerBus.notifyNoteDataSetChanged(note.groupUid)

        return insertResult.takeStatusWith(Unit)
    }

    fun loadNote(uid: UUID): OperationResult<Note> {
        return dbRepo.noteRepository.getNoteByUid(uid)
    }

    suspend fun updateNote(note: Note): OperationResult<Unit> =
        updateNoteUseCase.updateNote(note)

    fun loadTemplate(templateUid: UUID): Template? {
        val templates = dbRepo.templateRepository?.templates ?: return null
        return templates.firstOrNull { template -> template.uid == templateUid }
    }
}