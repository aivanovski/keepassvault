package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import java.util.*
import kotlinx.coroutines.withContext

class GroupsInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val observerBus: ObserverBus,
    private val dispatchers: DispatcherProvider
) {

    fun getTemplates(): List<Template>? {
        return dbRepo.templateRepository?.templates
    }

    fun getRootUid(): UUID? {
        val rootResult = dbRepo.groupRepository.rootGroup
        if (rootResult.isFailed) {
            return null
        }

        return rootResult.obj.uid
    }

    fun getRootGroupData(): OperationResult<List<Item>> {
        val rootGroupResult = dbRepo.groupRepository.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val groupUid = rootGroupResult.obj.uid

        return getGroupData(groupUid)
    }

    fun getGroupData(groupUid: UUID): OperationResult<List<Item>> {
        val groupsResult = dbRepo.groupRepository.getChildGroups(groupUid)
        if (groupsResult.isFailed) {
            return groupsResult.takeError()
        }

        val notesResult = dbRepo.noteRepository.getNotesByGroupUid(groupUid)
        if (notesResult.isFailed) {
            return groupsResult.takeError()
        }

        val groups = groupsResult.obj
        val notes = notesResult.obj

        val items = mutableListOf<Item>()
        for (group in groups) {
            val noteCountResult = dbRepo.noteRepository.getNoteCountByGroupUid(group.uid)
            if (noteCountResult.isFailed) {
                return noteCountResult.takeError()
            }

            val childGroupCountResult = dbRepo.groupRepository.getChildGroupsCount(group.uid)
            if (childGroupCountResult.isFailed) {
                return childGroupCountResult.takeError()
            }

            val noteCount = noteCountResult.obj
            val childGroupCount = childGroupCountResult.obj

            items.add(GroupItem(group, noteCount, childGroupCount))
        }

        for (note in notes) {
            items.add(NoteItem(note))
        }

        return OperationResult.success(items)
    }

    fun removeGroup(groupUid: UUID): OperationResult<Unit> {
        val removeResult = dbRepo.groupRepository.remove(groupUid)

        observerBus.notifyGroupDataSetChanged()

        return removeResult.takeStatusWith(Unit)
    }

    fun removeNote(groupUid: UUID, noteUid: UUID): OperationResult<Unit> {
        val removeResult = dbRepo.noteRepository.remove(noteUid)

        observerBus.notifyNoteDataSetChanged(groupUid)

        return removeResult.takeStatusWith(Unit)
    }

    suspend fun getGroup(groupUid: UUID): OperationResult<Group> {
        return withContext(dispatchers.IO) {
            dbRepo.groupRepository.getGroupByUid(groupUid)
        }
    }

    fun closeDatabaseIfNeed() {
        if (dbRepo.isOpened) {
            dbRepo.close()
        }
    }

    abstract class Item
    data class GroupItem(val group: Group, val noteCount: Int, val childGroupCount: Int) : Item()
    data class NoteItem(val note: Note) : Item()
}