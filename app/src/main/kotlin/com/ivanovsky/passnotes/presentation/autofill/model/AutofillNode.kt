package com.ivanovsky.passnotes.presentation.autofill.model

import android.app.assist.AssistStructure

data class AutofillNode(
    val type: AutofillFieldType,
    val sourceType: AutofillSourceType,
    val node: AssistStructure.ViewNode
)