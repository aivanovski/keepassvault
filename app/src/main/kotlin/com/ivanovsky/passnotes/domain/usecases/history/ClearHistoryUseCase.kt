package com.ivanovsky.passnotes.domain.usecases.history

import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.util.toOperationResult
import java.util.UUID
import kotlinx.coroutines.withContext

class ClearHistoryUseCase(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase
) {

    suspend fun clearHistory(noteUid: UUID): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            either {
                val db = getDbUseCase.getDatabaseSynchronously().toEither().bind()
                db.noteDao.setHistory(noteUid, emptyList()).toEither().bind()
            }.toOperationResult()
        }
}
