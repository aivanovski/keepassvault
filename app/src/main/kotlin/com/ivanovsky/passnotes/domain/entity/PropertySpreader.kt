package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import java.util.*

class PropertySpreader(properties: List<Property>) {

	val visibleProperties = properties.filter { property -> isPropertyVisible(property) }
			.toList()
			.sortedBy { property -> PROPERTY_TYPE_ORDER[property.type] }

	val hiddenProperties = properties.filter { property -> !isPropertyVisible(property) }
			.toList()

	private fun isPropertyVisible(property: Property): Boolean {
		return property.name.isNotEmpty() &&
				property.value.isNotEmpty() &&
				property.type != null &&
				property.type in VISIBLE_PROPERTY_TYPES
	}

	companion object {

		private val VISIBLE_PROPERTY_TYPES = EnumSet.of(PropertyType.PASSWORD,
				PropertyType.USER_NAME,
				PropertyType.NOTES,
				PropertyType.URL)

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