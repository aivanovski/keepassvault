package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

class FilterByNameStrategy(
    vararg names: String
) : PropertyFilterStrategy {

    private val nameSet = names.toSet()

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { property -> nameSet.contains(property.name) }
    }
}