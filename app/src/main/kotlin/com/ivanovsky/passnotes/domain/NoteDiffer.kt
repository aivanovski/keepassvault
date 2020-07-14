package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.*

class NoteDiffer {

    enum class NoteField {
        UID,
        GROUP_UID,
        CREATED,
        MODIFIED,
        TITLE,
        PROPERTIES
    }

    fun isEqualsByFields(lhs: Note, rhs: Note, fields: List<NoteField>): Boolean {
        for (field in fields) {
            val isEquals = when (field) {
                UID -> isUidEquals(lhs, rhs)
                GROUP_UID -> isGroupUidEquals(lhs, rhs)
                CREATED -> isCreatedEquals(lhs, rhs)
                MODIFIED -> isModifiedEquals(lhs, rhs)
                TITLE -> isTitleEquals(lhs, rhs)
                PROPERTIES -> isPropertiesEquals(lhs, rhs)
            }

            if (!isEquals) {
                return false
            }
        }

        return true
    }

    private fun isUidEquals(lhs: Note, rhs: Note): Boolean {
        return lhs.uid == rhs.uid
    }

    private fun isGroupUidEquals(lhs: Note, rhs: Note): Boolean {
        return lhs.groupUid == rhs.groupUid
    }

    private fun isCreatedEquals(lhs: Note, rhs: Note): Boolean {
        return lhs.created == rhs.created
    }

    private fun isModifiedEquals(lhs: Note, rhs: Note): Boolean {
        return lhs.modified == rhs.modified
    }

    private fun isTitleEquals(lhs: Note, rhs: Note): Boolean {
        return lhs.title == rhs.title
    }

    private fun isPropertiesEquals(lhs: Note, rhs: Note): Boolean {
        val equalsProperties = mutableListOf<Property>()
        for (lhsProp in lhs.properties) {
            if (rhs.properties.contains(lhsProp)) {
                equalsProperties.add(lhsProp)
            }
        }

        val lhsProps = lhs.properties.filter { property -> !equalsProperties.contains(property) }
        val rhsProps = rhs.properties.filter { property -> !equalsProperties.contains(property) }

        val lhsPropMap = lhsProps.map { property -> property.name to property }
            .toMap()

        val rhsPropMap = rhsProps.map { property -> property.name to property }
            .toMap()

        if (lhsPropMap.size != rhsPropMap.size) {
            return false
        }

        for (lhsProp in lhsPropMap.values) {
            val rhsProp = rhsPropMap[lhsProp.name]

            if (lhsProp != rhsProp) {
                return false
            }
        }

        // TODO: add comparision for properties with type == null

        return true
    }

    companion object {
        val ALL_FIELDS_WITHOUT_MODIFIED = listOf(UID, GROUP_UID, CREATED, TITLE, PROPERTIES)
    }
}
