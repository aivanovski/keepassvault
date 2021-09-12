package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext
import java.util.UUID

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
            val getNoteResult = db.noteRepository.getNoteByUid(noteUid)
            if (getNoteResult.isFailed) {
                return@withContext getNoteResult.takeError()
            }

            val note = getNoteResult.obj
            val updateNoteResult = db.noteRepository.update(
                note.copy(
                    groupUid = newGroupUid
                )
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