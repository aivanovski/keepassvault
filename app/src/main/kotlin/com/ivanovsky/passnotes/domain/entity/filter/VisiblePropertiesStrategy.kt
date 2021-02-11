package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

open class VisiblePropertiesStrategy : PropertyFilterStrategy {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { isPropertyVisible(it)}
    }

    protected fun isPropertyVisible(property: Property): Boolean {
        return property.name != Property.PROPERTY_NAME_TEMPLATE_UID
    }
}