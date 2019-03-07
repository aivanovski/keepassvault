package com.ivanovsky.passnotes.domain.interactor.group

import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.OperationStatus.*
import com.ivanovsky.passnotes.data.repository.GroupRepository

class GroupInteractor(private val context: Context,
                      private val groupRepository: GroupRepository,
                      private val observerBus: ObserverBus) {

	fun createNewGroup(title: String): OperationResult<Group> {
		val result = OperationResult<Group>()

		if (isTitleFree(title)) {
			val group = Group()
			group.title = title

			val insertResult = groupRepository.insert(group)
			when (insertResult.status) {
				SUCCEEDED, DEFERRED -> {
					observerBus.notifyGroupDataSetChanged()
					result.obj = group
				}
				FAILED -> {
					result.error = insertResult.error
				}
			}
		} else {
			result.error = newGenericError(context.getString(R.string.group_with_this_name_is_already_exist))
		}

		return result
	}

	private fun isTitleFree(title: String): Boolean {
		val groups = groupRepository.allGroup
		return groups.isSucceededOrDeferred
				&& groups.obj.none { group -> group.title == title }
	}
}