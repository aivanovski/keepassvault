package com.ivanovsky.passnotes.presentation.noteEditor

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.Template
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteEditorArgs(
    val mode: NoteEditorMode,
    val groupUid: UUID? = null,
    val noteUid: UUID? = null,
    val template: Template? = null,
    val title: String? = null,
    val properties: List<Property>? = null
) : Parcelable