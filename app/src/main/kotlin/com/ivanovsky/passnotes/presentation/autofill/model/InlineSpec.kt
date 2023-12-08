package com.ivanovsky.passnotes.presentation.autofill.model

import android.annotation.TargetApi
import android.os.Build
import android.os.Parcelable
import android.service.autofill.FillRequest
import android.view.inputmethod.InlineSuggestionsRequest
import kotlinx.parcelize.Parcelize

sealed class InlineSpec : Parcelable {

    @Parcelize
    object NotSpecified : InlineSpec()

    @Parcelize
    @TargetApi(30)
    data class InlineData(
        val data: InlineSuggestionsRequest
    ) : InlineSpec()

    companion object {

        fun fromAutofillRequest(request: FillRequest): InlineSpec {
            val data = if (Build.VERSION.SDK_INT >= 30) {
                request.inlineSuggestionsRequest
            } else {
                null
            }

            return if (data != null) {
                InlineData(data)
            } else {
                NotSpecified
            }
        }
    }
}