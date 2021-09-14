package com.ivanovsky.passnotes.domain.interactor.group_editor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetGroupUseCase
import kotlinx.coroutines.withContext
import java.util.UUID

class GroupEditorInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val getDbUseCase: GetDatabaseUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val resourceProvider: ResourceProvider,
    private val observerBus: ObserverBus,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getGroup(groupUid: UUID): OperationResult<Group> =
        getGroupUseCase.getGroupByUid(groupUid)

    suspend fun createNewGroup(title: String, parentUid: UUID): OperationResult<Group> {
        return withContext(dispatchers.IO) {
            if (!isTitleFree(title)) {
                return@withContext OperationResult.error(
                    newGenericError(resourceProvider.getString(R.string.group_with_this_name_is_already_exist))
                )
            }

            val group = Group()
            group.title = title

            val insertResult = dbRepo.groupRepository.insert(group, parentUid)
            if (insertResult.isFailed) {
                return@withContext insertResult.takeError()
            }

            observerBus.notifyGroupDataSetChanged()
            insertResult.takeStatusWith(group)
        }
    }

    suspend fun updateGroup(group: Group): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.groupRepository.update(group, null)
        }
    }

    private fun isTitleFree(title: String): Boolean {
        val groups = dbRepo.groupRepository.allGroup
        return groups.isSucceededOrDeferred
                && groups.obj.none { group -> group.title == title }
    }
}