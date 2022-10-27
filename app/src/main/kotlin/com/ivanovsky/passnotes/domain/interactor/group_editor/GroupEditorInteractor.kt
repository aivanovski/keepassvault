package com.ivanovsky.passnotes.domain.interactor.group_editor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_PARENT_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetGroupUseCase
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import kotlinx.coroutines.withContext
import java.util.UUID

class GroupEditorInteractor(
    private val getDbUseCase: GetDatabaseUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val resourceProvider: ResourceProvider,
    private val observerBus: ObserverBus,
    private val dispatchers: DispatcherProvider
) {

    suspend fun loadData(
        groupUid: UUID?,
        parentGroupUid: UUID?
    ): OperationResult<Pair<Group?, Group>> =
        withContext(dispatchers.IO) {
            val group = if (groupUid != null) {
                val getGroupResult = getGroupUseCase.getGroupByUid(groupUid)
                if (getGroupResult.isFailed) {
                    return@withContext getGroupResult.mapError()
                }

                getGroupResult.obj
            } else {
                null
            }

            val uid = when {
                group?.parentUid != null -> group.parentUid
                parentGroupUid != null -> parentGroupUid
                else -> {
                    return@withContext OperationResult.error(
                        newGenericError(MESSAGE_PARENT_UID_IS_NULL)
                    )
                }
            }

            val getParentResult = getGroupUseCase.getGroupByUid(uid)
            if (getParentResult.isFailed) {
                return@withContext getParentResult.mapError()
            }

            val parentGroup = getParentResult.obj

            OperationResult.success(Pair(group, parentGroup))
        }

    suspend fun createNewGroup(entity: GroupEntity): OperationResult<Group> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }
            val db = getDbResult.obj

            if (!isTitleAvailable(entity.title)) {
                return@withContext OperationResult.error(
                    newGenericError(resourceProvider.getString(R.string.group_with_this_name_is_already_exist))
                )
            }

            val insertResult = db.groupDao.insert(entity)
            if (insertResult.isFailed) {
                return@withContext insertResult.mapError()
            }

            val uid = insertResult.obj

            observerBus.notifyGroupDataSetChanged()

            val getGroupResult = db.groupDao.getGroupByUid(uid)
            if (getGroupResult.isFailed) {
                return@withContext getGroupResult.mapError()
            }

            insertResult.mapWithObject(getGroupResult.obj)
        }
    }

    suspend fun updateGroup(group: GroupEntity): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.groupDao.update(group)
        }
    }

    private fun isTitleAvailable(title: String): Boolean {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return false
        }

        val db = getDbResult.obj
        val groups = db.groupDao.all
        return groups.isSucceededOrDeferred &&
            groups.obj.none { group -> group.title == title }
    }
}