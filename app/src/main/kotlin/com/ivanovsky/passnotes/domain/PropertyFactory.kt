package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType

object PropertyFactory {

    fun createUsernameProperty(username: String): Property =
        Property(
            type = PropertyType.USER_NAME,
            name = PropertyType.USER_NAME.propertyName,
            value = username,
            isProtected = false
        )

    fun createPasswordProperty(password: String): Property =
        Property(
            type = PropertyType.PASSWORD,
            name = PropertyType.PASSWORD.propertyName,
            value = password,
            isProtected = true
        )

    fun createUrlProperty(url: String): Property =
        Property(
            type = PropertyType.URL,
            name = PropertyType.URL.propertyName,
            value = url,
            isProtected = false
        )

    fun createAutofillAppIdProperty(appId: String): Property =
        Property(
            type = null,
            name = Property.PROPERTY_NAME_AUTOFILL_APP_ID,
            value = appId,
            isProtected = false
        )
}