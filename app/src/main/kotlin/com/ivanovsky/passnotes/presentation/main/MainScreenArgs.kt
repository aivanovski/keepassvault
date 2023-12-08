package com.ivanovsky.passnotes.presentation.main

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.NoteCandidate
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillParams
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainScreenArgs(
    val appMode: ApplicationLaunchMode,
    val autofillParams: AutofillParams? = null,
    val note: NoteCandidate? = null
) : Parcelable