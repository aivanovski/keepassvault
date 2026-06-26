package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import java.util.UUID

class KeepassRsGroupDao(
    private val writableDao: GroupDao
) : GroupDao {

    override fun getAll(): OperationResult<List<Group>> = writableDao.all

    override fun getRootGroup(): OperationResult<Group> = writableDao.rootGroup

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<List<Group>> =
        writableDao.getChildGroups(parentGroupUid)

    override fun insert(group: GroupEntity): OperationResult<UUID> = writableDao.insert(group)

    override fun insert(group: GroupEntity, doCommit: Boolean): OperationResult<UUID> =
        writableDao.insert(group, doCommit)

    override fun remove(groupUid: UUID): OperationResult<Boolean> = writableDao.remove(groupUid)

    override fun getGroupByUid(groupUid: UUID): OperationResult<Group> =
        writableDao.getGroupByUid(groupUid)

    override fun update(group: GroupEntity, doCommit: Boolean): OperationResult<Boolean> =
        writableDao.update(group, doCommit)

    override fun find(query: String): OperationResult<List<Group>> = writableDao.find(query)

    override fun getContentWatcher(): ContentWatcher<Group> = writableDao.contentWatcher
}
