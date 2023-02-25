package com.ivanovsky.passnotes.domain

import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.ATTACHMENTS
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.CREATED
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.GROUP_UID
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.MODIFIED
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.PROPERTIES
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.TITLE
import com.ivanovsky.passnotes.domain.NoteDiffer.NoteField.UID
import java.util.LinkedList

class NoteDiffer {

    fun isEqualsByFields(lhs: Note, rhs: Note, fields: List<NoteField>): Boolean {
        for (field in fields) {
            val isEquals = when (field) {
                UID -> isUidEquals(lhs, rhs)
                GROUP_UID -> isGroupUidEquals(lhs, rhs)
                CREATED -> isCreatedEquals(lhs, rhs)
                MODIFIED -> isModifiedEquals(lhs, rhs)
                TITLE -> isTitleEquals(lhs, rhs)
                PROPERTIES -> isPropertiesEquals(lhs, rhs)
                ATTACHMENTS -> isAttachmentsEqual(lhs, rhs)
            }

            if (!isEquals) {
                return false
            }
        }

        return true
    }

    fun getAttachmentsDiff(lhs: Note, rhs: Note): List<Pair<DiffAction, Attachment>> {
        val lhsUids = lhs.attachments
            .map { attachment -> attachment.uid }
            .let { uids -> LinkedList(uids) }

        val lhsUidToAttachmentMap = lhs.attachments
            .associateBy { attachment -> attachment.uid }

        val rhsUids = rhs.attachments
            .map { attachment -> attachment.uid }
            .toMutableList()

        val rhsUidToAttachmentMap = rhs.attachments
            .associateBy { attachment -> attachment.uid }

        val diff = mutableListOf<Pair<DiffAction, Attachment>>()

        while (lhsUids.isNotEmpty()) {
            val lhsUid = lhsUids.removeFirst()
            val attachment = lhsUidToAttachmentMap[lhsUid] ?: continue

            if (lhsUid !in rhsUids) {
                diff.add(
                    Pair(
                        DiffAction.REMOVE,
                        attachment
                    )
                )
            } else {
                rhsUids.remove(lhsUid)
            }
        }

        for (rhsUid in rhsUids) {
            val attachment = rhsUidToAttachmentMap[rhsUid] ?: continue

            diff.add(
                Pair(
                    DiffAction.INSERT,
                    attachment
                )
            )
        }

        return diff
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

    private fun isAttachmentsEqual(lhs: Note, rhs: Note): Boolean {
        if (lhs.attachments.size != rhs.attachments.size) {
            return false
        }

        val lhsUids = lhs.attachments.map { it.uid }
        val rhsUids = rhs.attachments.map { it.uid }

        return lhsUids == rhsUids
    }

    enum class NoteField {
        UID,
        GROUP_UID,
        CREATED,
        MODIFIED,
        TITLE,
        PROPERTIES,
        ATTACHMENTS
    }

    enum class DiffAction {
        INSERT,
        REMOVE
    }

    companion object {
        val ALL_FIELDS_WITHOUT_MODIFIED =
            listOf(UID, GROUP_UID, CREATED, TITLE, PROPERTIES, ATTACHMENTS)
    }
}