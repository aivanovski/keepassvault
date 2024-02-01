package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import java.util.UUID
import kotlinx.coroutines.withContext

class MoveNoteUseCase(
    private val dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus,
    private val getDbUseCase: GetDatabaseUseCase
) {

    suspend fun moveNote(noteUid: UUID, newGroupUid: UUID): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val getNoteResult = db.noteDao.getNoteByUid(noteUid)
            if (getNoteResult.isFailed) {
                return@withContext getNoteResult.takeError()
            }

            val note = getNoteResult.obj
            val updateNoteResult = db.noteDao.update(
                note.copy(groupUid = newGroupUid),
                true
            )
            if (updateNoteResult.isFailed) {
                return@withContext updateNoteResult.takeError()
            }

            observerBus.notifyNoteDataSetChanged(note.groupUid)
            observerBus.notifyNoteDataSetChanged(newGroupUid)

            OperationResult.success(true)
        }
    }
}