package com.ivanovsky.passnotes.presentation.autofill.model

import android.os.Parcelable
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import kotlinx.parcelize.Parcelize

@Parcelize
data class AutofillField(
    val type: AutofillFieldType?,
    val autofillId: AutofillId?,
    val autofillValue: AutofillValue?
) : Parcelable