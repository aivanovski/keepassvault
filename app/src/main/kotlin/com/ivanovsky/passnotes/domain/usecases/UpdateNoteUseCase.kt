package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class UpdateNoteUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val observerBus: ObserverBus,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun updateNote(note: Note): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val updateResult = db.noteRepository.update(note)
            if (updateResult.isFailed) {
                return@withContext updateResult.takeError()
            }

            val groupUid = note.groupUid
            val oldUid = note.uid
            val newUid = updateResult.obj

            observerBus.notifyNoteContentChanged(groupUid, oldUid, newUid)

            updateResult.takeStatusWith(Unit)
        }
}