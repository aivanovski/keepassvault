package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import java.util.UUID
import kotlinx.coroutines.withContext

class FindParentGroupsUseCase(
    private val dispatchers: DispatcherProvider,
    private val getDbUseCase: GetDatabaseUseCase
) {

    suspend fun findAllParents(
        groupUid: UUID,
        isAddCurrentGroup: Boolean
    ): OperationResult<List<Group>> {
        return withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabaseSynchronously()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val getAllGroupsResult = getDbResult.getOrThrow().groupDao.all
            if (getAllGroupsResult.isFailed) {
                return@withContext getAllGroupsResult.mapError()
            }

            val allGroups = getAllGroupsResult.getOrThrow()
            val rootGroup = allGroups.firstOrNull { group -> group.parentUid == null }
                ?: return@withContext OperationResult.error(
                    OperationError.newDbError(OperationError.MESSAGE_FAILED_TO_FIND_ROOT_GROUP)
                )

            if (groupUid == rootGroup.uid) {
                return@withContext OperationResult.success(listOf(rootGroup))
            }

            val selectedGroup = allGroups.firstOrNull { group -> group.uid == groupUid }
                ?: return@withContext OperationResult.error(
                    OperationError.newDbError(
                        String.format(
                            OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            groupUid
                        )
                    )
                )

            val uidToGroupMap = allGroups.associateBy { group -> group.uid }
            val uidToParentUidMap: Map<UUID, UUID?> = HashMap<UUID, UUID?>()
                .apply {
                    for (group in allGroups) {
                        this[group.uid] = group.parentUid
                    }
                }

            val parents = mutableListOf<Group>()
                .apply {
                    if (isAddCurrentGroup) {
                        add(selectedGroup)
                    }
                }

            var currentUid: UUID? = if (isAddCurrentGroup) {
                groupUid
            } else {
                uidToParentUidMap[groupUid]
            }

            while (currentUid != null) {
                val parentUid = uidToParentUidMap[currentUid]
                if (parentUid != null) {
                    val parentGroup = uidToGroupMap[parentUid]
                    if (parentGroup != null) {
                        parents.add(parentGroup)
                    }
                }

                currentUid = parentUid
            }

            OperationResult.success(parents.reversed())
        }
    }
}