package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import java.util.*

class GroupsInteractor(dbRepo: EncryptedDatabaseRepository) {

    private val groupRepository = dbRepo.groupRepository
    private val noteRepository = dbRepo.noteRepository

    fun getRootUid(): UUID? {
        val rootResult = groupRepository.rootGroup
        if (rootResult.isFailed) {
            return null
        }

        return rootResult.obj.uid
    }

    fun getRootGroupData(): OperationResult<List<Item>> {
        val rootGroupResult = groupRepository.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val groupUid = rootGroupResult.obj.uid

        return getGroupData(groupUid)
    }

    fun getGroupData(groupUid: UUID): OperationResult<List<Item>> {
        val groupsResult = groupRepository.getChildGroups(groupUid)
        if (groupsResult.isFailed) {
            return groupsResult.takeError()
        }

        val notesResult = noteRepository.getNotesByGroupUid(groupUid)
        if (notesResult.isFailed) {
            return groupsResult.takeError()
        }

        val groups = groupsResult.obj
        val notes = notesResult.obj

        val items = mutableListOf<Item>()
        for (group in groups) {
            val noteCountResult = noteRepository.getNoteCountByGroupUid(group.uid)
            if (noteCountResult.isFailed) {
                return noteCountResult.takeError()
            }

            val childGroupCountResult = groupRepository.getChildGroupsCount(group.uid)
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

    abstract class Item
    data class GroupItem(val group: Group, val noteCount: Int, val childGroupCount: Int) : Item()
    data class NoteItem(val note: Note) : Item()
}