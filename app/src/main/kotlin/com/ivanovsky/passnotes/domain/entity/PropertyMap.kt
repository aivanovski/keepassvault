package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.map.PropertyMapByName
import com.ivanovsky.passnotes.domain.entity.map.PropertyMapByPropertyType

interface PropertyMap<T> {

    fun get(param: T): Property?

    companion object {

        fun mapByType(properties: List<Property>): PropertyMap<PropertyType> {
            return PropertyMapByPropertyType(properties)
        }

        fun mapByName(properties: List<Property>): PropertyMap<String> {
            return PropertyMapByName(properties)
        }
    }
}