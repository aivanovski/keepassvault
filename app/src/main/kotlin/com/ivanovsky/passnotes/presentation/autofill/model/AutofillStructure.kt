package com.ivanovsky.passnotes.presentation.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AutofillStructure(
    val applicationId: String?,
    val isWebView: Boolean,
    val webDomain: String?,
    val username: AutofillField?,
    val password: AutofillField?
) : Parcelable {

    fun hasFieldsToFill(): Boolean {
        return username != null || password != null
    }
}