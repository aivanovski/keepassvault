package com.ivanovsky.passnotes.presentation.group_editor

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

sealed class GroupEditorArgs : Parcelable {
    @Parcelize
    data class NewGroup(val parentGroupUid: UUID) : GroupEditorArgs()
    @Parcelize
    data class EditGroup(val groupUid: UUID) : GroupEditorArgs()
}