package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

class NotEmptyPropertiesStrategy : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { property ->
            !property.value.isNullOrEmpty() && !property.name.isNullOrEmpty()
        }
    }
}