package com.ivanovsky.passnotes.presentation.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Context
import android.os.Build
import android.text.InputType
import android.view.View
import androidx.annotation.RequiresApi
import com.ivanovsky.passnotes.presentation.autofill.extensions.getNodesByFieldType
import com.ivanovsky.passnotes.presentation.autofill.extensions.hasFields
import com.ivanovsky.passnotes.presentation.autofill.extensions.toField
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillFieldType
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillNode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillSourceType
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.autofill.model.MutableAutofillStructure
import java.util.Locale
import timber.log.Timber

@RequiresApi(api = 26)
class AutofillStructureParser(
    private val context: Context
) {

    fun parse(sourceStructure: AssistStructure): AutofillStructure? {
        val structure = MutableAutofillStructure()

        for (windowNode in sourceStructure.getWindowNodes()) {
            val applicationId = windowNode.title.toString().split("/").firstOrNull()

            if (applicationId == context.packageName) {
                continue
            }

            if (applicationId?.contains("PopupWindow:") == false) {
                structure.applicationId = applicationId

                parseViewNode(structure, windowNode.rootViewNode)

                val result = processResult(structure)
                Timber.d("structure: $structure")
                Timber.d("result: $result")

                return result
            }
        }

        return null
    }

    private fun parseViewNode(result: MutableAutofillStructure, node: ViewNode) {
        Timber.d(
            "parseViewNode: className=%s, hints=%s, htmlAttrs=%s, inputType=0x%s, hint=%s",
            node.className,
            node.autofillHints?.toList(),
            node.htmlInfo?.attributes,
            Integer.toHexString(node.inputType),
            node.hint
        )

        if (node.className == "android.webkit.WebView") {
            result.isWebView = true
        }

        if (node.webDomain?.isNotEmpty() == true) {
            result.webDomain = node.webDomain
            Timber.d("Autofill domain: ${node.webDomain}")
        }
        if (Build.VERSION.SDK_INT >= 28 && node.webScheme?.isNotEmpty() == true) {
            result.webScheme = node.webScheme
            Timber.d("Autofill scheme: ${node.webScheme}")
        }

        if (node.visibility != View.VISIBLE) {
            return
        }

        if (node.autofillId != null) {
            val nodeFromAutofill = getAutofillNodeByAutofillHint(node)
            if (nodeFromAutofill != null) {
                Timber.d("    dataFromAutofill: $nodeFromAutofill")
                result.nodes.add(nodeFromAutofill)
                return
            }

            val nodeFromHtml = getAutofillNodeByHtmlAttributes(node)
            if (nodeFromHtml != null) {
                Timber.d("    dataFromHtml: $nodeFromHtml")
                result.nodes.add(nodeFromHtml)
                return
            }

            val nodeFromInputType = getAutofillNodeByInputType(node)
            if (nodeFromInputType != null) {
                Timber.d("    dataFromInputType: $nodeFromInputType")
                result.nodes.add(nodeFromInputType)
                return
            }
        }

        for (i in 0 until node.childCount) {
            parseViewNode(result, node.getChildAt(i))

            Timber.d(
                "check: className=%s, result.hasWebDomain=%s, result.hasFieldsToFill=%s",
                node.className,
                result.hasWebDomain(),
                result.hasFields()
            )
        }
    }

    private fun processResult(result: MutableAutofillStructure): AutofillStructure? {
        if (!result.hasFields()) {
            return null
        }

        val username = chooseBestNode(result.getNodesByFieldType(AutofillFieldType.USERNAME))
        val password = chooseBestNode(result.getNodesByFieldType(AutofillFieldType.PASSWORD))

        if (username == null || password == null) {
            return null
        }

        return AutofillStructure(
            applicationId = result.applicationId,
            isWebView = result.isWebView,
            webDomain = result.webDomain,
            username = username.toField(),
            password = password.toField()
        )
    }

    private fun chooseBestNode(nodes: List<AutofillNode>): AutofillNode? {
        if (nodes.size == 1) {
            return nodes.first()
        }

        return nodes.map { node ->
            val autofillScore = getAutofillNodeByAutofillHint(node.node)?.sourceType?.priority ?: 0
            val htmlScore = getAutofillNodeByHtmlAttributes(node.node)?.sourceType?.priority ?: 0
            val inputTypeScore = getAutofillNodeByInputType(node.node)?.sourceType?.priority ?: 0
            val hintScore = getAutofillNodeByEditTextHint(node.node)?.sourceType?.priority ?: 0

            Pair(autofillScore + htmlScore + inputTypeScore + hintScore, node)
        }
            .maxByOrNull { it.first }
            ?.second
    }

    private fun AssistStructure.getWindowNodes(): List<AssistStructure.WindowNode> {
        return (0 until windowNodeCount).map { getWindowNodeAt(it) }
    }

    private fun MutableAutofillStructure.hasWebDomain(): Boolean {
        return webDomain?.isNotEmpty() == true
    }

    private fun ViewNode.hasValidData(): Boolean {
        val id = autofillId
        val value = autofillValue
        return id != null || value != null // TODO(autofill): check content inside id and value
    }

    private fun getAutofillNodeByAutofillHint(node: ViewNode): AutofillNode? {
        val hints = node.autofillHints ?: return null

        if (!node.hasValidData()) {
            return null
        }

        for (hint in hints) {
            if (hint == null) {
                continue
            }

            when {
                isAutofillHintMatchUsername(hint) -> return AutofillNode(
                    AutofillFieldType.USERNAME,
                    AutofillSourceType.AUTOFILL_HINT,
                    node
                )
                isAutofillHintMatchPassword(hint) -> return AutofillNode(
                    AutofillFieldType.PASSWORD,
                    AutofillSourceType.AUTOFILL_HINT,
                    node
                )
                // TODO(autofill): add more options
            }
        }

        return null
    }

    private fun isAutofillHintMatchUsername(hint: String): Boolean {
        return hint.contains(View.AUTOFILL_HINT_USERNAME, true) ||
            hint.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, true) ||
            hint.contains("email", true) ||
            hint.contains(View.AUTOFILL_HINT_PHONE, true)
    }

    private fun isAutofillHintMatchPassword(hint: String): Boolean {
        return hint.contains(View.AUTOFILL_HINT_PASSWORD, true)
    }

    private fun getAutofillNodeByHtmlAttributes(node: ViewNode): AutofillNode? {
        val nodeHtml = node.htmlInfo ?: return null

        if (!node.hasValidData()) {
            return null
        }

        if (nodeHtml.tag.lowercase(Locale.ENGLISH) != "input") {
            return null
        }

        val attributes = nodeHtml.attributes ?: return null
        for (attribute in attributes) {
            val type = attribute.first ?: continue

            if (type.equals("type", ignoreCase = true)) {
                continue
            }

            val name = attribute.second ?: continue
            when {
                name.equals("tel", ignoreCase = true) ||
                    name.equals("email", ignoreCase = true) ||
                    name.equals("text", ignoreCase = true) ||
                    name.contains("user", ignoreCase = true) -> {
                    return AutofillNode(
                        AutofillFieldType.USERNAME,
                        AutofillSourceType.HTML_ATTRIBUTE,
                        node
                    )
                }
                name.equals("password", ignoreCase = true) -> {
                    return AutofillNode(
                        AutofillFieldType.PASSWORD,
                        AutofillSourceType.HTML_ATTRIBUTE,
                        node
                    )
                }
            }
        }

        return null
    }

    private fun isInputTypeMatchWith(inputType: Int, types: Collection<Int>): Boolean {
        return types.any { type -> isInputTypeMatchWith(inputType, type) }
    }

    private fun isInputTypeMatchWith(inputType: Int, type: Int): Boolean {
        return inputType and InputType.TYPE_MASK_VARIATION == type
    }

    private fun getAutofillNodeByInputType(node: ViewNode): AutofillNode? {
        val inputType = node.inputType

        if (!node.hasValidData()) {
            return null
        }

        return when {
            isInputTypeMatchUsername(inputType) -> AutofillNode(
                AutofillFieldType.USERNAME,
                AutofillSourceType.INPUT_TYPE,
                node
            )
            isInputTypeMatchPassword(inputType) -> AutofillNode(
                AutofillFieldType.PASSWORD,
                AutofillSourceType.INPUT_TYPE,
                node
            )
            else -> null
        }
    }

    private fun getInputTypeClass(inputType: Int): Int {
        return inputType and InputType.TYPE_MASK_CLASS
    }

    private fun isInputTypeMatchUsername(inputType: Int): Boolean {
        val isText = getInputTypeClass(inputType) == InputType.TYPE_CLASS_TEXT
        if (isText && isInputTypeMatchWith(inputType, USERNAME_INPUT_TYPES)) {
            return true
        }

        val isNumber = getInputTypeClass(inputType) == InputType.TYPE_CLASS_NUMBER
        return isNumber && isInputTypeMatchWith(inputType, InputType.TYPE_NUMBER_VARIATION_NORMAL)
    }

    private fun isInputTypeMatchPassword(inputType: Int): Boolean {
        val isText = getInputTypeClass(inputType) == InputType.TYPE_CLASS_TEXT
        if (isText && isInputTypeMatchWith(inputType, PASSWORD_INPUT_TYPES)) {
            return true
        }

        val isNumber = getInputTypeClass(inputType) == InputType.TYPE_CLASS_NUMBER
        return isNumber &&
            isInputTypeMatchWith(inputType, InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun getAutofillNodeByEditTextHint(node: ViewNode): AutofillNode? {
        if (!node.hasValidData()) {
            return null
        }

        val hint = node.hint ?: return null

        return when {
            hint.contains("username", ignoreCase = true) ||
                hint.contains("email", ignoreCase = true) -> {
                AutofillNode(
                    AutofillFieldType.USERNAME,
                    AutofillSourceType.EDIT_TEXT_HINT,
                    node
                )
            }
            hint.contentEquals("password", ignoreCase = true) -> {
                AutofillNode(
                    AutofillFieldType.USERNAME,
                    AutofillSourceType.EDIT_TEXT_HINT,
                    node
                )
            }
            else -> null
        }
    }

    companion object {
        private val USERNAME_INPUT_TYPES = setOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_NORMAL,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
        )

        private val PASSWORD_INPUT_TYPES = setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )

        private val TAG = AutofillStructureParser::class.java.simpleName
    }
}