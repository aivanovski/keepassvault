package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.keepass.TemplateParser
import java.util.*

@Deprecated("") // TODO: Refactor class
class PropertySpreader(
    private val properties: List<Property>
) {

    fun getVisibleProperties(): List<Property> {
        return properties.filter { property -> isPropertyVisible(property) }
            .toList()
            .sortedBy { property ->
                if (property.type == null) {
                    Integer.MAX_VALUE
                } else {
                    PROPERTY_TYPE_ORDER[property.type]
                }
            }
    }

    fun getHiddenProperties(): List<Property> {
        return properties.filter { property -> !isPropertyVisible(property) }
    }

    fun getVisiblePropertyValueByType(propertyType: PropertyType): String? {
        return getVisibleProperties().firstOrNull { property -> property.type == propertyType }
            ?.value
    }

    fun getCustomProperties(): List<Property> {
        return getVisibleProperties().filter { property ->
            !property.name.isNullOrEmpty() &&
                    (property.type == null || !DEFAULT_PROPERTIES.contains(property.type))
        }
    }

    fun excludeTemplateRelatedProperties(template: Template): List<Property> {
        val excludeNames = template.fields.map { field -> field.title }
        return getCustomProperties().filter { property -> !excludeNames.contains(property.name) }
    }

    fun getVisibleNotEmptyWithoutTitle(): List<Property> {
        return getVisibleProperties().filter { property ->
            property.type != PropertyType.TITLE && !property.name.isNullOrEmpty() && !property.value.isNullOrEmpty()
        }
    }

    fun getTemplateProperties(): List<Property> {
        return getVisibleProperties().filter { property -> isTemplateProperty(property) }
    }

    private fun isTemplateProperty(property: Property): Boolean {
        return property.name != null &&
                (property.name == Property.PROPERTY_NAME_TEMPLATE ||
                        TEMPLATE_PROPERTY_PREFIXES.any { prefix -> property.name.startsWith(prefix) })
    }

    private fun isPropertyVisible(property: Property): Boolean {
        return property.name != Property.PROPERTY_NAME_TEMPLATE_UID
    }

    fun getPropertyByName(name: String): Property? {
        return properties.firstOrNull { property -> name == property.name }
    }

    fun getPropertyValueOrNull(name: String): String? {
        return properties.firstOrNull { property -> name == property.name }?.value
    }

    fun getPropertiesWithPrefix(prefix: String): List<Property> {
        return properties.filter { property ->
            property.name != null && property.name.startsWith(prefix)
        }
    }

    fun findTemplateUid(): String? {
        return getPropertyValueOrNull(Property.PROPERTY_NAME_TEMPLATE_UID)
    }

    fun hasTemplateUidProperty(): Boolean {
        return getPropertyByName(Property.PROPERTY_NAME_TEMPLATE_UID) != null
    }

    companion object {

        private val TEMPLATE_PROPERTY_PREFIXES = setOf(
            TemplateParser.PROPERTY_PREFIX_POSITION,
            TemplateParser.PROPERTY_PREFIX_TITLE,
            TemplateParser.PROPERTY_PREFIX_TYPE
        )

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