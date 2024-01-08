package com.ivanovsky.passnotes.presentation.groups

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.NoteCandidate
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupsScreenArgs(
    val appMode: ApplicationLaunchMode,
    val groupUid: UUID?,
    val isCloseDatabaseOnExit: Boolean,
    val isSearchModeEnabled: Boolean,
    val autofillStructure: AutofillStructure? = null,
    val note: NoteCandidate? = null
) : Parcelable