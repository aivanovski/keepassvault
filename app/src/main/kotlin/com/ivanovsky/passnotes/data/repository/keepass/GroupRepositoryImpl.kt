package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.GroupEntity
import java.util.UUID

class GroupRepositoryImpl(private val dao: GroupDao) : GroupRepository {

    override fun getGroupByUid(groupUid: UUID): OperationResult<Group> {
        return dao.getGroupByUid(groupUid)
    }

    override fun getAllGroup(): OperationResult<List<Group>> {
        return dao.all
    }

    override fun getRootGroup(): OperationResult<Group> {
        return dao.rootGroup
    }

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<List<Group>> {
        return dao.getChildGroups(parentGroupUid)
    }

    override fun insert(group: GroupEntity): OperationResult<UUID> {
        return dao.insert(group)
    }

    override fun remove(groupUid: UUID): OperationResult<Boolean> {
        return dao.remove(groupUid)
    }

    override fun find(query: String): OperationResult<List<Group>> {
        val allGroupsResult = dao.all
        if (allGroupsResult.isFailed) {
            return allGroupsResult.takeError()
        }

        val allGroups = allGroupsResult.obj
        val matchedGroups = allGroups
            .filter { group ->
                group.title.contains(query, ignoreCase = true)
            }

        return OperationResult.success(matchedGroups)
    }

    override fun update(group: GroupEntity): OperationResult<Boolean> {
        return dao.update(group)
    }
}