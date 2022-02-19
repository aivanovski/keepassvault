package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType

class ExcludeByTypeStrategy(
    vararg types: PropertyType
) : PropertyFilterStrategy {

    private val excludeTypes = types.toSet()

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { it.type != null && !excludeTypes.contains(it.type) }
    }
}