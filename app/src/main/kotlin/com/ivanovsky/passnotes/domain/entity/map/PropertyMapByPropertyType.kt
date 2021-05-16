package com.ivanovsky.passnotes.domain.entity.map

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.PropertyMap

class PropertyMapByPropertyType(
    private val properties: List<Property>
) : PropertyMap<PropertyType> {

    override fun get(param: PropertyType): Property? {
        // TODO: can be optimized
        return properties.firstOrNull { it.type == param }
    }
}