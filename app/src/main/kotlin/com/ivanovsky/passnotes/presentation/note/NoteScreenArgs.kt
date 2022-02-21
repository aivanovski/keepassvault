package com.ivanovsky.passnotes.presentation.note

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class NoteScreenArgs(
    val appMode: ApplicationLaunchMode,
    val noteUid: UUID,
    val autofillStructure: AutofillStructure? = null
) : Parcelable