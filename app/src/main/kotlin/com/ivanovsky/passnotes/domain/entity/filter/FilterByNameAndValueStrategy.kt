package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

class FilterByNameAndValueStrategy(
    private val name: String,
    private val value: String
) : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { it.name == name && it.value == value }
    }
}