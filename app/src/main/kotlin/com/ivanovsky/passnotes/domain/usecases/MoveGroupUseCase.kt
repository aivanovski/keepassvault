package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext
import java.util.UUID

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
           val getGroupResult = db.groupRepository.getGroupByUid(groupUid)
           if (getGroupResult.isFailed) {
               return@withContext getGroupResult.takeError()
           }

           val group = getGroupResult.obj
           val moveResult = db.groupRepository.update(group, newParentGroupUid)
           if (moveResult.isFailed) {
               return@withContext moveResult.takeError();
           }

           observerBus.notifyGroupDataSetChanged()

           OperationResult.success(true)
       }
    }
}