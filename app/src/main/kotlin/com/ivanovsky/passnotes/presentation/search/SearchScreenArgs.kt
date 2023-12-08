package com.ivanovsky.passnotes.presentation.search

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillParams
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchScreenArgs(
    val appMode: ApplicationLaunchMode,
    val autofillParams: AutofillParams? = null
) : Parcelable