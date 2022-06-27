package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext
import java.util.UUID

class GetNoteUseCase(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getNoteByUid(noteUid: UUID): OperationResult<Note> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.noteDao.getNoteByUid(noteUid)
        }
    }
}