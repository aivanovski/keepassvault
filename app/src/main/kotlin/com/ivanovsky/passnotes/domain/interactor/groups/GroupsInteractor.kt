package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.data.repository.NoteRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class GroupsInteractor(private val groupRepository: GroupRepository,
                       private val noteRepository: NoteRepository) {

	fun getAllGroupsWithNoteCount(): Single<List<Pair<Group, Int>>> {
		return groupRepository.allGroup
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.map { groups -> createGroupToNoteCountPairs(groups)}
	}

	private fun createGroupToNoteCountPairs(groups: List<Group>): List<Pair<Group, Int>> {
		val result = mutableListOf<Pair<Group, Int>>()

		for (group in groups) {
			val noteCount = noteRepository.getNoteCountByGroupUid(group.uid)
			result.add(Pair(group, noteCount))
		}

		return result
	}
}