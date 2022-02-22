package com.ivanovsky.passnotes.presentation.autofill.extensions

import androidx.annotation.RequiresApi
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillField
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillNode
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillFieldType
import com.ivanovsky.passnotes.presentation.autofill.model.MutableAutofillStructure

fun MutableAutofillStructure.getNodesByFieldType(type: AutofillFieldType): List<AutofillNode> {
    return nodes.filter { it.type == type }
}

fun MutableAutofillStructure.hasFields(): Boolean {
    val usernames = getNodesByFieldType(AutofillFieldType.USERNAME)
    val passwords = getNodesByFieldType(AutofillFieldType.PASSWORD)
    return usernames.isNotEmpty() && passwords.isNotEmpty()
}

@RequiresApi(api = 26)
fun AutofillNode.toField(): AutofillField {
    return AutofillField(
        type,
        node.autofillId,
        node.autofillValue
    )
}