package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

class HiddenPropertiesStrategy : VisiblePropertiesStrategy() {

    override fun apply(properties: Sequence<Property>): Sequence<Property> {
        return properties.filter { !isPropertyVisible(it)}
    }
}