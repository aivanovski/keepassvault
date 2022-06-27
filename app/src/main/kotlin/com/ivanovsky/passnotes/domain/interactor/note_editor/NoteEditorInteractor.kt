package com.ivanovsky.passnotes.domain.interactor.note_editor

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteUseCase
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteEditorInteractor(
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus,
) {

    fun createNewNote(note: Note): OperationResult<Unit> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.takeError()
        }

        val db = getDbResult.obj
        val insertResult = db.noteDao.insert(note)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        observerBus.notifyNoteDataSetChanged(note.groupUid)

        return insertResult.takeStatusWith(Unit)
    }

    fun loadNote(uid: UUID): OperationResult<Note> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.takeError()
        }

        val db = getDbResult.obj
        return db.noteDao.getNoteByUid(uid)
    }

    suspend fun updateNote(note: Note): OperationResult<Unit> =
        updateNoteUseCase.updateNote(note)

    suspend fun loadTemplate(templateUid: UUID): OperationResult<Template?> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val getTemplatesResult = db.templateDao.getTemplates()
            if (getTemplatesResult.isFailed) {
                return@withContext getTemplatesResult.takeError()
            }

            val templates = getTemplatesResult.obj
            val template = templates.firstOrNull { template -> template.uid == templateUid }

            OperationResult.success(template)
        }
}