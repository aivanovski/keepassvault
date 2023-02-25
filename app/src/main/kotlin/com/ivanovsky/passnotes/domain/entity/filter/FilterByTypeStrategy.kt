package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType

class FilterByTypeStrategy(
    vararg types: PropertyType
) : PropertyFilterStrategy {

    private val typeSet = types.toSet()

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { property -> typeSet.contains(property.type) }
    }
}