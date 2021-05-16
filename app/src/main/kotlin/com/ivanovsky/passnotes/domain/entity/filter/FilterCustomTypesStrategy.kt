package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.entity.filter.FilterDefaultTypesStrategy.Companion.DEFAULT_TYPES

class FilterCustomTypesStrategy : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { !DEFAULT_TYPES.contains(it.type) }
    }
}