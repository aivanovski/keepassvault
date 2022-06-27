package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.entity.SelectionItem
import com.ivanovsky.passnotes.domain.entity.SelectionItemType
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.domain.usecases.AddTemplatesUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseStatusUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.MoveGroupUseCase
import com.ivanovsky.passnotes.domain.usecases.MoveNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.SortGroupsAndNotesUseCase
import kotlinx.coroutines.withContext
import java.util.UUID

class GroupsInteractor(
    private val observerBus: ObserverBus,
    private val dispatchers: DispatcherProvider,
    private val lockUseCase: LockDatabaseUseCase,
    private val getStatusUseCase: GetDatabaseStatusUseCase,
    private val addTemplatesUseCase: AddTemplatesUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val moveGroupUseCae: MoveGroupUseCase,
    private val sortUseCase: SortGroupsAndNotesUseCase,
    private val getDbUseCase: GetDatabaseUseCase
) {

    suspend fun getTemplates(): OperationResult<List<Template>> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.templateDao.getTemplates()
        }

    fun getRootUid(): UUID? {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return null
        }

        val db = getDbResult.obj
        val rootResult = db.groupRepository.rootGroup
        if (rootResult.isFailed) {
            return null
        }

        return rootResult.obj.uid
    }

    fun getRootGroupData(): OperationResult<List<EncryptedDatabaseEntry>> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.takeError()
        }

        val db = getDbResult.obj

        val rootGroupResult = db.groupRepository.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val groupUid = rootGroupResult.obj.uid

        return getGroupData(groupUid)
    }

    fun getGroupData(groupUid: UUID): OperationResult<List<EncryptedDatabaseEntry>> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.takeError()
        }

        val db = getDbResult.obj
        val groupsResult = db.groupRepository.getChildGroups(groupUid)
        if (groupsResult.isFailed) {
            return groupsResult.takeError()
        }

        val notesResult = db.noteRepository.getNotesByGroupUid(groupUid)
        if (notesResult.isFailed) {
            return groupsResult.takeError()
        }

        val groups = groupsResult.obj
        val notes = notesResult.obj

        return OperationResult.success(groups + notes)
    }

    suspend fun sortData(
        data: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> =
        sortUseCase.sortGroupsAndNotesAccordingToSettings(data)

    fun removeGroup(groupUid: UUID): OperationResult<Unit> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.takeError()
        }

        val db = getDbResult.obj
        val removeResult = db.groupRepository.remove(groupUid)

        observerBus.notifyGroupDataSetChanged()

        return removeResult.takeStatusWith(Unit)
    }

    fun removeNote(groupUid: UUID, noteUid: UUID): OperationResult<Unit> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.takeError()
        }

        val db = getDbResult.obj
        val removeResult = db.noteRepository.remove(noteUid)

        observerBus.notifyNoteDataSetChanged(groupUid)

        return removeResult.takeStatusWith(Unit)
    }

    suspend fun getGroup(groupUid: UUID): OperationResult<Group> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.groupRepository.getGroupByUid(groupUid)
        }
    }

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }

    suspend fun getDatabaseStatus(): OperationResult<DatabaseStatus> =
        getStatusUseCase.getDatabaseStatus()

    suspend fun addTemplates(): OperationResult<Boolean> =
        addTemplatesUseCase.addTemplates()

    suspend fun doActionOnSelection(
        selectedGroupUid: UUID,
        action: SelectionHolder.ActionType,
        selection: SelectionItem
    ): OperationResult<Boolean> {
        return when (action) {
            SelectionHolder.ActionType.CUT -> {
                when (selection.type) {
                    SelectionItemType.NOTE_UID -> moveNoteUseCase.moveNote(
                        selection.uid,
                        selectedGroupUid
                    )
                    SelectionItemType.GROUP_UID -> moveGroupUseCae.moveGroup(
                        selection.uid,
                        selectedGroupUid
                    )
                }
            }
        }
    }
}