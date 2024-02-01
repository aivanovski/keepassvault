package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import java.util.UUID
import kotlinx.coroutines.withContext

class MoveGroupUseCase(
    private val dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus,
    private val getDbUseCase: GetDatabaseUseCase
) {

    suspend fun moveGroup(groupUid: UUID, newParentGroupUid: UUID): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            val getGroupResult = db.groupDao.getGroupByUid(groupUid)
            if (getGroupResult.isFailed) {
                return@withContext getGroupResult.takeError()
            }

            val group = getGroupResult.obj
            val newGroup = GroupEntity(
                uid = groupUid,
                parentUid = newParentGroupUid,
                title = group.title,
                autotypeEnabled = group.autotypeEnabled,
                searchEnabled = group.searchEnabled
            )
            val moveResult = db.groupDao.update(newGroup, true)
            if (moveResult.isFailed) {
                return@withContext moveResult.takeError()
            }

            observerBus.notifyGroupDataSetChanged()

            OperationResult.success(true)
        }
    }
}