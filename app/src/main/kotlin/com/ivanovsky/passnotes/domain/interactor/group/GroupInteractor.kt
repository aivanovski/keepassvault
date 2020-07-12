package com.ivanovsky.passnotes.domain.interactor.group

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.ResourceHelper
import java.util.*

class GroupInteractor(
    dbRepos: EncryptedDatabaseRepository,
    private val resourceHelper: ResourceHelper,
    private val observerBus: ObserverBus
) {
    private val groupRepository = dbRepos.groupRepository

    fun getRootGroupUid(): OperationResult<UUID> {
        val rootGroupResult = groupRepository.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val rootUid = rootGroupResult.obj.uid
        return rootGroupResult.takeStatusWith(rootUid)
    }

    fun createNewGroup(title: String, parentUid: UUID): OperationResult<Group> {
        if (!isTitleFree(title)) {
            return OperationResult.error(
                newGenericError(resourceHelper.getString(R.string.group_with_this_name_is_already_exist))
            )
        }

        val group = Group()
        group.title = title

        val insertResult = groupRepository.insert(group, parentUid)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        observerBus.notifyGroupDataSetChanged()
        return insertResult.takeStatusWith(group)
    }

    private fun isTitleFree(title: String): Boolean {
        val groups = groupRepository.allGroup
        return groups.isSucceededOrDeferred
                && groups.obj.none { group -> group.title == title }
    }
}