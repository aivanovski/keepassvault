package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Property(
    val type: PropertyType? = null,
    val name: String? = null,
    val value: String? = null,
    val isProtected: Boolean = false
) : EncryptedDatabaseElement, Parcelable {

    companion object {
        const val PROPERTY_NAME_TEMPLATE = "_etm_template"
        const val PROPERTY_NAME_TEMPLATE_UID = "_etm_template_uuid"

        const val PROPERTY_NAME_AUTOFILL_APP_ID = "AndroidApp"

        const val PROPERTY_VALUE_TEMPLATE = "1"
    }
}