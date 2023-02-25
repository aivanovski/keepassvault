package com.ivanovsky.passnotes.presentation.group_editor

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupEditorArgs(
    val mode: GroupEditorMode,
    val parentGroupUid: UUID? = null,
    val groupUid: UUID? = null
) : Parcelable {

    init {
        checkArguments()
    }

    private fun checkArguments() {
        if (mode == GroupEditorMode.NEW && parentGroupUid == null) {
            throw IllegalArgumentException()
        }
        if (mode == GroupEditorMode.EDIT && groupUid == null) {
            throw IllegalArgumentException()
        }
    }

    companion object {

        fun newGroupArgs(parentGroupUid: UUID): GroupEditorArgs =
            GroupEditorArgs(
                mode = GroupEditorMode.NEW,
                parentGroupUid = parentGroupUid
            )

        fun editGroupArgs(groupUid: UUID): GroupEditorArgs =
            GroupEditorArgs(
                mode = GroupEditorMode.EDIT,
                groupUid = groupUid
            )
    }
}