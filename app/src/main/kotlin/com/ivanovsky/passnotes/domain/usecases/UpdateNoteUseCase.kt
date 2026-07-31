package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_NOTE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import kotlinx.coroutines.withContext

class UpdateNoteUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val observerBus: ObserverBus,
    private val dispatchers: DispatcherProvider
) {

    suspend fun updateNote(note: Note): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            if (note.uid == null) {
                return@withContext OperationResult.error(
                    newDbError(
                        MESSAGE_NOTE_UID_IS_NULL,
                        Stacktrace()
                    )
                )
            }

            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val updateResult = db.noteDao.update(note, true)
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