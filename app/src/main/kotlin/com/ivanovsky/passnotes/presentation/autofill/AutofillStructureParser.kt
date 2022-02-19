package com.ivanovsky.passnotes.presentation.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.text.InputType
import android.view.View
import androidx.annotation.RequiresApi
import com.ivanovsky.passnotes.presentation.autofill.model.HintType
import com.ivanovsky.passnotes.presentation.autofill.model.HintData
import com.ivanovsky.passnotes.presentation.autofill.model.MutableAutofillStructure
import com.ivanovsky.passnotes.util.Logger
import java.util.Locale

@RequiresApi(api = 26)
class AutofillStructureParser {

    fun parse(structure: AssistStructure): MutableAutofillStructure? {
        val result = MutableAutofillStructure()

        for (windowNode in structure.getWindowNodes()) {
            val applicationId = windowNode.title.toString().split("/").firstOrNull()

            if (applicationId?.contains("PopupWindow:") == false) {
                if (parseViewNode(result, windowNode.rootViewNode)) {
                    Logger.d(TAG, "result: $result")
                    return result
                }
            }
        }

        return null
    }

    private fun AssistStructure.getWindowNodes(): List<AssistStructure.WindowNode> {
        return (0 until windowNodeCount).map { getWindowNodeAt(it) }
    }

    private fun parseViewNode(result: MutableAutofillStructure, node: ViewNode): Boolean {
        Logger.d(
            TAG,
            "parseViewNode: className=%s, autofillHints=%s, htmlAttributes=%s, inputType=0x%s",
            node.className,
            node.autofillHints?.toList(),
            node.htmlInfo?.attributes,
            Integer.toHexString(node.inputType)
        )

        if (node.className == "android.webkit.WebView") {
            result.isWebView = true
        }

        if (node.webDomain?.isNotEmpty() == true) {
            result.webDomain = node.webDomain
            Logger.d(TAG, "Autofill domain: ${node.webDomain}")
        }
        if (Build.VERSION.SDK_INT >= 28 && node.webScheme?.isNotEmpty() == true) {
            result.webScheme = node.webScheme
            Logger.d(TAG, "Autofill scheme: ${node.webScheme}")
        }

        if (node.visibility != View.VISIBLE) {
            return false
        }

        if (node.autofillId != null) {
            val autofillHintData = getHintDataByAutofillHint(node)
            if (autofillHintData != null) {
                fillResult(result, autofillHintData)
                Logger.d(TAG, "    data: $autofillHintData")
                return true
            }

            val htmlHintData = getHintDataByHtmlAttributes(node)
            if (htmlHintData != null) {
                fillResult(result, htmlHintData)
                Logger.d(TAG, "    data: $htmlHintData")
                return true
            }

            val inputHintData = getHintDataByInputType(node)
            if (inputHintData != null) {
                fillResult(result, inputHintData)
                Logger.d(TAG, "    data: $inputHintData")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            parseViewNode(result, node.getChildAt(i))

            Logger.d(
                TAG, "check: className=${node.className}, canBeEnded=${result.hasWebDomain()}, " +
                    "resultHasData=${result.hasFieldsToFill()}"
            )

            if (result.hasWebDomain() && result.hasFieldsToFill()) {
                return true
            }
        }

        return false
    }

    private fun MutableAutofillStructure.hasWebDomain(): Boolean {
        return webDomain?.isNotEmpty() == true
    }

    private fun ViewNode.hasValidData(): Boolean {
        val id = autofillId
        val value = autofillValue
        return id != null || value != null // TODO(autofill): check content inside id and value
    }

    private fun ViewNode.getHintData(type: HintType): HintData? {
        return if (hasValidData()) {
            HintData(type, autofillId, autofillValue)
        } else {
            null
        }
    }

    private fun getHintDataByAutofillHint(node: ViewNode): HintData? {
        val hints = node.autofillHints ?: return null

        if (!node.hasValidData()) {
            return null
        }

        for (hint in hints) {
            when {
                isAutofillHintMatchUsername(hint) -> return node.getHintData(HintType.USERNAME)
                isAutofillHintMatchPassword(hint) -> return node.getHintData(HintType.PASSWORD)
                // TODO(autofill): add more options
            }
        }

        return null
    }

    private fun fillResult(result: MutableAutofillStructure, hintData: HintData) {
        when (hintData.type) {
            HintType.PASSWORD -> {
                result.password = hintData
            }
            HintType.USERNAME -> {
                result.username = hintData
            }
            // TODO(autofill): add more options
        }
    }

    private fun isAutofillHintMatchUsername(hint: String): Boolean {
        return hint.contains(View.AUTOFILL_HINT_USERNAME, true)
            || hint.contains(View.AUTOFILL_HINT_EMAIL_ADDRESS, true)
            || hint.contains("email", true)
            || hint.contains(View.AUTOFILL_HINT_PHONE, true)
    }

    private fun isAutofillHintMatchPassword(hint: String): Boolean {
        return hint.contains(View.AUTOFILL_HINT_PASSWORD, true)
    }

    private fun getHintDataByHtmlAttributes(node: ViewNode): HintData? {
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
                name.equals("tel", ignoreCase = true)
                    || name.equals("email", ignoreCase = true)
                    || name.equals("text", ignoreCase = true) -> {
                    return node.getHintData(HintType.USERNAME)
                }
                name.equals("password", ignoreCase = true) -> {
                    return node.getHintData(HintType.PASSWORD)
                }
            }
        }

        return null
    }

    private fun isInputTypeMatchWith(inputType: Int, vararg types: Int): Boolean {
        return types.any { type ->
            inputType and InputType.TYPE_MASK_VARIATION == type
        }
    }

    private fun getHintDataByInputType(node: ViewNode): HintData? {
        val inputType = node.inputType

        if (!node.hasValidData()) {
            return null
        }

        return when {
            isInputTypeMatchUsername(inputType) -> node.getHintData(HintType.USERNAME)
            isInputTypeMatchPassword(inputType) -> node.getHintData(HintType.PASSWORD)
            else -> null
        }
    }

    private fun getInputTypeClass(inputType: Int): Int {
        return inputType and InputType.TYPE_MASK_CLASS
    }

    private fun isInputTypeMatchUsername(inputType: Int): Boolean {
        return (getInputTypeClass(inputType) == InputType.TYPE_CLASS_TEXT &&
            isInputTypeMatchWith(
                inputType,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_NORMAL,
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
            )) ||
            (getInputTypeClass(inputType) == InputType.TYPE_CLASS_NUMBER &&
                isInputTypeMatchWith(inputType, InputType.TYPE_NUMBER_VARIATION_NORMAL))
    }

    private fun isInputTypeMatchPassword(inputType: Int): Boolean {
        return (getInputTypeClass(inputType) == InputType.TYPE_CLASS_TEXT &&
            isInputTypeMatchWith(
                inputType,
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )) ||
            (getInputTypeClass(inputType) == InputType.TYPE_CLASS_NUMBER &&
                isInputTypeMatchWith(inputType, InputType.TYPE_NUMBER_VARIATION_PASSWORD))
    }

    companion object {
        private val TAG = AutofillStructureParser::class.java.simpleName
    }
}