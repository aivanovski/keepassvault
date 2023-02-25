package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import java.util.EnumMap

class SortedByTypeStrategy : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.sortedBy { property ->
            if (property.type == null) {
                Integer.MAX_VALUE
            } else {
                PROPERTY_TYPE_ORDER[property.type]
            }
        }
    }

    companion object {

        private val PROPERTY_TYPE_ORDER =
            EnumMap<PropertyType, Int>(PropertyType::class.java).apply {
                put(PropertyType.USER_NAME, 1)
                put(PropertyType.PASSWORD, 2)
                put(PropertyType.URL, 3)
                put(PropertyType.NOTES, 4)
            }
    }
}