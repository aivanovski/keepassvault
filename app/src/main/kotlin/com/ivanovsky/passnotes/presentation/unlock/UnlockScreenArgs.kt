package com.ivanovsky.passnotes.presentation.unlock

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnlockScreenArgs(
    val appMode: ApplicationLaunchMode,
    val autofillStructure: AutofillStructure? = null
) : Parcelable