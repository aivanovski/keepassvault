package com.ivanovsky.passnotes.presentation.note

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteScreenArgs(
    val appMode: ApplicationLaunchMode,
    val noteSource: NoteSource,
    val autofillStructure: AutofillStructure? = null,
    val isViewOnly: Boolean = false
) : Parcelable

sealed class NoteSource : Parcelable {

    @Parcelize
    data class ByUid(val uid: UUID) : NoteSource()

    @Parcelize
    data class ByNote(val note: Note) : NoteSource()

    fun getNoteUid(): UUID? {
        return when (this) {
            is ByUid -> uid
            is ByNote -> note.uid
        }
    }
}