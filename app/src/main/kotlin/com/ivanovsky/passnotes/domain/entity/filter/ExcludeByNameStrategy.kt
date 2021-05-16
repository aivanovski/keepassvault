package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

class ExcludeByNameStrategy(
    vararg names: String
) : PropertyFilterStrategy {

    private val excludedNames = names.toSet()

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { !excludedNames.contains(it.name) }
    }
}