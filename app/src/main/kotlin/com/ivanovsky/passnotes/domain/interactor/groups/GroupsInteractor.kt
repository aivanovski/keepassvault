package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.data.repository.NoteRepository

class GroupsInteractor(private val groupRepository: GroupRepository,
                       private val noteRepository: NoteRepository) {

	fun getAllGroupsWithNoteCount(): OperationResult<List<Pair<Group, Int>>> {
		val result: OperationResult<List<Pair<Group, Int>>>

		val groups = groupRepository.allGroup
		if (groups.result != null) {
			result = createGroupToNoteCountPairs(groups)
		} else {
			result = OperationResult.error(groups.error)
		}

		return result
	}

	private fun createGroupToNoteCountPairs(groupsResult: OperationResult<List<Group>>): OperationResult<List<Pair<Group, Int>>> {
		val pairs = mutableListOf<Pair<Group, Int>>()
		var error: OperationError? = null

		for (group in groupsResult.result) {
			val noteCountResult = noteRepository.getNoteCountByGroupUid(group.uid)

			if (noteCountResult.result != null) {
				pairs.add(Pair(group, noteCountResult.result))
			} else {
				error = noteCountResult.error
				break
			}
		}

		return if (error == null) OperationResult.success(pairs) else OperationResult.error(error)
	}
}