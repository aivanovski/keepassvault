package com.ivanovsky.passnotes.domain.entity.map

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.entity.PropertyMap

class PropertyMapByName(
    private val properties: List<Property>
) : PropertyMap<String> {

    override fun get(param: String): Property? {
        return properties.firstOrNull { it.name == param }
    }
}