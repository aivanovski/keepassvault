package com.ivanovsky.passnotes.presentation.autofill.model

enum class AutofillSourceType(val priority: Int) {
    EDIT_TEXT_HINT(0b1),
    INPUT_TYPE(0b10),
    HTML_ATTRIBUTE(0b100),
    AUTOFILL_HINT(0b1000)
}