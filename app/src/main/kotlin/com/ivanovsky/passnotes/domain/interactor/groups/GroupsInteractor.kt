package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.data.repository.NoteRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class GroupsInteractor(private val groupRepository: GroupRepository,
                       private val noteRepository: NoteRepository) {

	fun getAllGroupsWithNoteCount(): Single<OperationResult<List<Pair<Group, Int>>>> {
		return Single.fromCallable { groupRepository.allGroup }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map { groupsResult ->
					val result: OperationResult<List<Pair<Group, Int>>>

					if (groupsResult.result != null) {
						result = createGroupToNoteCountPairs(groupsResult)
					} else {
						result = OperationResult.error(groupsResult.error)
					}

					result
				}
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