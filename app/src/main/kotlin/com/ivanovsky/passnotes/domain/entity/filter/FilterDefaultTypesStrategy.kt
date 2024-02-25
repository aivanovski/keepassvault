package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType

class FilterDefaultTypesStrategy : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { DEFAULT_TYPES.contains(it.type) }
    }

    companion object {
        val DEFAULT_TYPES = setOf(
            PropertyType.TITLE,
            PropertyType.USER_NAME,
            PropertyType.PASSWORD,
            PropertyType.OTP,
            PropertyType.URL,
            PropertyType.NOTES
        )
    }
}