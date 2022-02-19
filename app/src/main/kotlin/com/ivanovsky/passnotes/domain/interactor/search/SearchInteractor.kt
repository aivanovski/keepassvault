package com.ivanovsky.passnotes.domain.interactor.search

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import kotlinx.coroutines.withContext
import java.util.UUID

class SearchInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val lockUseCase: LockDatabaseUseCase,
) {

    suspend fun find(query: String): OperationResult<List<Item>> {
        return withContext(dispatchers.IO) {
            val dbResult = getDbUseCase.getDatabase()
            if (dbResult.isFailed) {
                return@withContext dbResult.takeError()
            }

            val db = dbResult.obj
            val notesResult = db.noteRepository.find(query)
            if (notesResult.isFailed) {
                return@withContext notesResult.takeError()
            }

            val groupsResult = db.groupRepository.find(query)
            if (groupsResult.isFailed) {
                return@withContext groupsResult.takeError()
            }

            val notes = notesResult.obj
            val groups = groupsResult.obj

            val items = mutableListOf<Item>()

            for (group in groups) {
                val noteCountResult = dbRepo.noteRepository.getNoteCountByGroupUid(group.uid)
                if (noteCountResult.isFailed) {
                    return@withContext noteCountResult.takeError()
                }

                val childGroupCountResult = dbRepo.groupRepository.getChildGroupsCount(group.uid)
                if (childGroupCountResult.isFailed) {
                    return@withContext childGroupCountResult.takeError()
                }

                val noteCount = noteCountResult.obj
                val childGroupCount = childGroupCountResult.obj

                items.add(
                    Item.GroupItem(
                        group = group,
                        noteCount = noteCount,
                        childGroupCount = childGroupCount
                    )
                )
            }

            items.addAll(notes.map { Item.NoteItem(it) })

            OperationResult.success(items)
        }
    }

    suspend fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        getNoteUseCase.getNoteByUid(noteUid)

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }

    sealed class Item {
        data class NoteItem(val note: Note) : Item()
        data class GroupItem(
            val group: Group,
            val noteCount: Int,
            val childGroupCount: Int
        ) : Item()
    }
}