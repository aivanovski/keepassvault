package com.ivanovsky.passnotes.domain.interactor.group_editor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetGroupUseCase
import kotlinx.coroutines.withContext
import java.util.UUID

class GroupEditorInteractor(
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
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }
            val db = getDbResult.obj

            if (!isTitleAvailable(title)) {
                return@withContext OperationResult.error(
                    newGenericError(resourceProvider.getString(R.string.group_with_this_name_is_already_exist))
                )
            }

            val group = GroupEntity(
                parentUid = parentUid,
                title = title
            )

            val insertResult = db.groupRepository.insert(group)
            if (insertResult.isFailed) {
                return@withContext insertResult.takeError()
            }

            val uid = insertResult.obj

            observerBus.notifyGroupDataSetChanged()

            val result = Group(
                uid = uid,
                parentUid = parentUid,
                title = title,
                groupCount = 0,
                noteCount = 0
            )

            insertResult.takeStatusWith(result)
        }
    }

    suspend fun updateGroup(group: GroupEntity): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.groupRepository.update(group)
        }
    }

    private fun isTitleAvailable(title: String): Boolean {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return false
        }

        val db = getDbResult.obj
        val groups = db.groupRepository.allGroup
        return groups.isSucceededOrDeferred &&
            groups.obj.none { group -> group.title == title }
    }
}