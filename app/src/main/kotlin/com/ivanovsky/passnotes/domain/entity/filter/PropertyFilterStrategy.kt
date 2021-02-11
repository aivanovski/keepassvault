package com.ivanovsky.passnotes.domain.entity.filter

import com.ivanovsky.passnotes.data.entity.Property

interface PropertyFilterStrategy {

    fun apply(properties: Sequence<Property>): Sequence<Property>
}