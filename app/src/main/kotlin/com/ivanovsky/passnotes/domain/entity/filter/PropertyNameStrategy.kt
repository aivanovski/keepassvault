package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

class PropertyNameStrategy(
    private val name: String
) : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { it.name == name }
    }
}