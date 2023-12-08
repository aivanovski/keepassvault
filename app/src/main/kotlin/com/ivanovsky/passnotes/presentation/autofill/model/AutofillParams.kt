package com.ivanovsky.passnotes.presentation.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AutofillParams(
    val structure: AutofillStructure,
    val inlineSpec: InlineSpec?
) : Parcelable