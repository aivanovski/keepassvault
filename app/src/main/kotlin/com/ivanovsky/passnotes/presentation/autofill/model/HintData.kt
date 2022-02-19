package com.ivanovsky.passnotes.presentation.autofill.model

import android.os.Parcelable
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import kotlinx.parcelize.Parcelize

@Parcelize
data class HintData(
    val type: HintType?,
    val autofillId: AutofillId?,
    val autofillValue: AutofillValue?
) : Parcelable