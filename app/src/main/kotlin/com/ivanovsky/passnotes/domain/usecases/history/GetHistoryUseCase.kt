package com.ivanovsky.passnotes.domain.usecases.history

import com.github.aivanovski.keepasstreediff.PathDiffer
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.diff.DiffSorter
import com.ivanovsky.passnotes.domain.usecases.diff.buildNodeTree
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.toInternalDiffEvent
import com.ivanovsky.passnotes.domain.usecases.history.entity.HistoryDiffItem
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import java.util.UUID
import kotlinx.coroutines.withContext

class GetHistoryUseCase(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase
) {

    suspend fun getHistoryDiff(noteUid: UUID): OperationResult<List<HistoryDiffItem>> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabaseSynchronously()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val db = getDbResult.getOrThrow()
            val getNoteResult = db.noteDao.getNoteByUid(noteUid)
            if (getNoteResult.isFailed) {
                return@withContext getNoteResult.mapError()
            }

            val getHistoryResult = db.noteDao.getHistory(noteUid)
            if (getHistoryResult.isFailed) {
                return@withContext getHistoryResult.mapError()
            }

            val note = getNoteResult.getOrThrow()
            val history = getHistoryResult.getOrThrow()
            val allHistory = history.plus(note).reversed()

            val result = mutableListOf<HistoryDiffItem>()
            for (idx in 1 until allHistory.size) {
                val newNote = allHistory[idx - 1]
                val oldNote = allHistory[idx]

                val diff = PathDiffer().diff(
                    lhs = oldNote.buildNodeTree(),
                    rhs = newNote.buildNodeTree()
                )
                    .map { event -> event.toInternalDiffEvent() }

                val sortedDiff = DiffSorter().sort(diff)

                result.add(
                    HistoryDiffItem(
                        oldNote = oldNote,
                        newNote = newNote,
                        diffEvents = sortedDiff as List<DiffEvent<Property>>
                    )
                )
            }

            OperationResult.success(result)
        }
}