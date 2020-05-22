package com.ivanovsky.passnotes.domain.interactor.groups

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.data.repository.NoteRepository

class GroupsInteractor(
    private val groupRepository: GroupRepository,
    private val noteRepository: NoteRepository
) {

    fun getAllGroupsWithNoteCount(): OperationResult<List<Pair<Group, Int>>> {
        val result: OperationResult<List<Pair<Group, Int>>>

        val groupsResult = groupRepository.allGroup
        if (groupsResult.isSucceededOrDeferred) {
            result = createGroupToNoteCountPairs(groupsResult.obj)
        } else {
            result = OperationResult.error(groupsResult.error)
        }

        return result
    }

    private fun createGroupToNoteCountPairs(groups: List<Group>): OperationResult<List<Pair<Group, Int>>> {
        val pairs = mutableListOf<Pair<Group, Int>>()
        var error: OperationError? = null

        for (group in groups) {
            val noteCountResult = noteRepository.getNoteCountByGroupUid(group.uid)

            if (noteCountResult.isSucceededOrDeferred) {
                pairs.add(Pair(group, noteCountResult.obj))
            } else {
                error = noteCountResult.error
                break
            }
        }

        return if (error == null) OperationResult.success(pairs) else OperationResult.error(error)
    }
}