package com.ivanovsky.passnotes.presentation.autofill.model

import androidx.annotation.RequiresApi
import java.lang.StringBuilder

data class MutableAutofillStructure(
    var applicationId: String? = null,
    var isWebView: Boolean = false,
    var webDomain: String? = null,
    var webScheme: String? = null,
    val nodes: MutableList<AutofillNode> = mutableListOf()
) {

    @RequiresApi(api = 26)
    override fun toString(): String {
        return StringBuilder(this::class.java.simpleName)
            .apply {
                append("applicationId=").append(applicationId).append(", ")
                append("isWebView=").append(isWebView).append(", ")
                append("webDomain=").append(webDomain).append(", ")
                append("webScheme=").append(webScheme).append(", ")

                append("nodes=[")

                for (node in nodes) {
                    append("AutofillNode(")
                    append("type=").append(node.type).append(", ")
                    append("sourceType=").append(node.sourceType).append(", ")
                    append("id=").append(node.node.autofillId).append(", ")
                    append("hints=").append(node.node.autofillHints?.toList()).append(", ")
                    append("text=").append(node.node.text).append(", ")
                    append("hint=").append(node.node.hint)
                    append("), ")
                }

                append("]")
            }
            .toString()
    }
}