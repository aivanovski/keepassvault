package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.PropertyType.NOTES
import com.ivanovsky.passnotes.data.entity.PropertyType.PASSWORD
import com.ivanovsky.passnotes.data.entity.PropertyType.TITLE
import com.ivanovsky.passnotes.data.entity.PropertyType.URL
import com.ivanovsky.passnotes.data.entity.PropertyType.USER_NAME
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

        val DEFAULT_PROPERTIES = listOf(
            Property(TITLE, TITLE.propertyName, "", isProtected = false),
            Property(PASSWORD, PASSWORD.propertyName, "", isProtected = true),
            Property(USER_NAME, USER_NAME.propertyName, "", isProtected = false),
            Property(URL, URL.propertyName, "", isProtected = false),
            Property(NOTES, NOTES.propertyName, "", isProtected = false)
        )
    }
}