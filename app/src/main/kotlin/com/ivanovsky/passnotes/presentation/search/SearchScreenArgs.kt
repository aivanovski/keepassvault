package com.ivanovsky.passnotes.presentation.search

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchScreenArgs(
    val appMode: ApplicationLaunchMode,
    val autofillStructure: AutofillStructure? = null
) : Parcelable