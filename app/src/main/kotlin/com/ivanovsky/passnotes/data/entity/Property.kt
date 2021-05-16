package com.ivanovsky.passnotes.data.entity

data class Property(
    val type: PropertyType? = null,
    val name: String? = null,
    val value: String? = null,
    val isProtected: Boolean = false
) {

    val isDefault = (type != null && PropertyType.DEFAULT_TYPES.contains(type))

    companion object {

        const val PROPERTY_NAME_TEMPLATE = "_etm_template"
        const val PROPERTY_NAME_TEMPLATE_UID = "_etm_template_uuid"

        const val PROPERTY_VALUE_TEMPLATE = "1"
    }
}