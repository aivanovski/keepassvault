package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationResult
import java.util.UUID

class GroupRepositoryWrapper : RepositoryWrapperWithDatabase(), GroupRepository {

    override fun getGroupByUid(groupUid: UUID): OperationResult<Group> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.getGroupByUid(groupUid)
    }

    override fun getAllGroup(): OperationResult<MutableList<Group>> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.allGroup
    }

    override fun getRootGroup(): OperationResult<Group> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.rootGroup
    }

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<MutableList<Group>> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.getChildGroups(parentGroupUid)
    }

    override fun getChildGroupsCount(parentGroupUid: UUID): OperationResult<Int> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.getChildGroupsCount(parentGroupUid)
    }

    override fun insert(group: GroupEntity): OperationResult<UUID> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.insert(group)
    }

    override fun remove(groupUid: UUID): OperationResult<Boolean> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.remove(groupUid)
    }

    override fun find(query: String): OperationResult<MutableList<Group>> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.find(query)
    }

    override fun update(group: GroupEntity): OperationResult<Boolean> {
        val db = getDatabase() ?: return noDatabaseError()
        return db.groupRepository.update(group)
    }
}