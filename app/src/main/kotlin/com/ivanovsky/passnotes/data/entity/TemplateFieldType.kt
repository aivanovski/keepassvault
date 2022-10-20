package com.ivanovsky.passnotes.data.entity

enum class TemplateFieldType(val textName: String) {

    INLINE("Inline"),
    PROTECTED_INLINE("Protected Inline"),
    DATE_TIME("Date Time");

    companion object {
        fun fromTextName(textName: String): TemplateFieldType? {
            return values().firstOrNull { item -> item.textName == textName }
        }
    }
}