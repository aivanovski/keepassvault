package com.ivanovsky.passnotes.presentation.autofill.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AutofillStructure(
    val isWebView: Boolean,
    val webDomain: String?,
    val webScheme: String?,
    val username: HintData?,
    val password: HintData?
) : Parcelable {

    fun hasFieldsToFill(): Boolean {
        return username != null || password != null
    }
}