package com.ivanovsky.passnotes.presentation.note_editor

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.Template
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class NoteEditorArgs(
    val launchMode: LaunchMode,
    val groupUid: UUID? = null,
    val noteUid: UUID? = null,
    val template: Template? = null,
    val title: String? = null
) : Parcelable