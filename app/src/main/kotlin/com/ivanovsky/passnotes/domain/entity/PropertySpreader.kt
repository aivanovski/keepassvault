package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import java.util.*

class PropertySpreader(properties: List<Property>) {

    private val visibleProperties = properties.filter { property -> isPropertyVisible(property) }
        .toList()
        .sortedBy { property ->
            if (property.type == null) {
                Integer.MAX_VALUE
            } else {
                PROPERTY_TYPE_ORDER[property.type]
            }
        }

    val hiddenProperties = properties.filter { property -> !isPropertyVisible(property) }
        .toList()

    fun getVisiblePropertyValueByType(propertyType: PropertyType): String? {
        return visibleProperties.firstOrNull { property -> property.type == propertyType }
            ?.value
    }

    fun getCustomProperties(): List<Property> {
        return visibleProperties.filter { property ->
            !property.name.isNullOrEmpty() &&
                    (property.type == null || !DEFAULT_PROPERTIES.contains(property.type))
        }
    }

    fun getVisibleNotEmptyWithoutTitle(): List<Property> {
        return visibleProperties.filter { property ->
            property.type != PropertyType.TITLE && !property.name.isNullOrEmpty() && !property.value.isNullOrEmpty()
        }
    }

    private fun isPropertyVisible(property: Property): Boolean {
        return true
    }

    companion object {

        private val DEFAULT_PROPERTIES = EnumSet.of(
            PropertyType.PASSWORD,
            PropertyType.USER_NAME,
            PropertyType.NOTES,
            PropertyType.URL,
            PropertyType.TITLE
        )

        private val PROPERTY_TYPE_ORDER = createPropertyTypeOrderMap()

        private fun createPropertyTypeOrderMap(): Map<PropertyType, Int> {
            val result = EnumMap<PropertyType, Int>(PropertyType::class.java)

            result.put(PropertyType.USER_NAME, 1)
            result.put(PropertyType.PASSWORD, 2)
            result.put(PropertyType.URL, 3)
            result.put(PropertyType.NOTES, 4)

            return result
        }
    }
}