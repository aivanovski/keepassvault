package com.ivanovsky.passnotes.domain.search

import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.SearchOptions
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.isPropertySearchable

class StrictEntryMatcher : EntryMatcher {

    override fun match(
        options: SearchOptions,
        query: String,
        entries: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> {
        return entries.filter { entry ->
            when (entry) {
                is Note -> isNoteMatch(options, entry, query)
                is Group -> isGroupMatch(options, entry, query)
            }
        }
    }

    private fun isNoteMatch(
        options: SearchOptions,
        note: Note,
        query: String
    ): Boolean {
        for (property in note.properties) {
            if (isPropertyMatch(options, property, query)) {
                return true
            }
        }

        for (attachment in note.attachments) {
            if (isAttachmentMatch(options, attachment, query)) {
                return true
            }
        }

        return false
    }

    private fun isAttachmentMatch(
        options: SearchOptions,
        attachment: Attachment,
        query: String
    ): Boolean {
        return if (options.isOtherFieldsEnabled) {
            attachment.name.contains(query, ignoreCase = !options.isCaseSensitive)
        } else {
            false
        }
    }

    private fun isPropertyMatch(
        options: SearchOptions,
        property: Property,
        query: String
    ): Boolean {
        if (!options.isPropertySearchable(property)) {
            return false
        }

        val name = property.name ?: StringUtils.EMPTY
        val value = property.value ?: StringUtils.EMPTY
        val isDefaultProperty = PropertyType.DEFAULT_TYPES.contains(property.type)

        val isValueMatch = value.contains(query, ignoreCase = !options.isCaseSensitive)
        val isNameMatch =
            !isDefaultProperty && name.contains(query, ignoreCase = !options.isCaseSensitive)

        return isValueMatch || isNameMatch
    }

    private fun isGroupMatch(
        options: SearchOptions,
        group: Group,
        query: String
    ): Boolean {
        return group.title.contains(query, ignoreCase = !options.isCaseSensitive)
    }
}