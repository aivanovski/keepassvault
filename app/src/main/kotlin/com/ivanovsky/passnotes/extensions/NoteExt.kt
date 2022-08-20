package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.entity.PropertyFilter

fun Note.addOrUpdateProperty(property: Property): Note {
    requireNotNull(property.name)

    val existing = PropertyFilter.Builder()
        .filterByName(property.name)
        .build()
        .apply(properties)

    val newProperties = properties.toMutableList()
    return if (existing.size != 1 || existing.firstOrNull() != property) {
        if (existing.size == 1) {
            val idx = newProperties.indexOf(existing.first())
            newProperties[idx] = property

        } else {
            if (existing.isNotEmpty()) {
                newProperties.removeAll(existing)
            }
            newProperties.add(property)
        }

        this.copy(
            properties = newProperties
        )
    } else {
        this
    }
}

fun Note.matches(query: String): Boolean {
    val isTitleMatches = title.contains(query, ignoreCase = true)
    val isPropertyMatches = properties.any { it.matches(query) }

    return isTitleMatches || isPropertyMatches
}
