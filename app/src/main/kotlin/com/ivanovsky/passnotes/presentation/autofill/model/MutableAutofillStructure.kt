package com.ivanovsky.passnotes.presentation.autofill.model

data class MutableAutofillStructure(
    var isWebView: Boolean = false,
    var webDomain: String? = null,
    var webScheme: String? = null,
    var username: HintData? = null,
    var password: HintData? = null
) {

    fun toAutofillStructure(): AutofillStructure {
        return AutofillStructure(
            isWebView,
            webDomain,
            webScheme,
            username,
            password
        )
    }
}