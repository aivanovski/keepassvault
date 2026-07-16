package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.otp.OtpUriFactory

fun determinePropertyType(name: String, value: String): PropertyType? {
    val type = PropertyType.getByName(name) ?: return null

    return if (type == PropertyType.OTP) {
        if (OtpUriFactory.parseUri(value) != null) {
            PropertyType.OTP
        } else {
            null
        }
    } else {
        type
    }
}