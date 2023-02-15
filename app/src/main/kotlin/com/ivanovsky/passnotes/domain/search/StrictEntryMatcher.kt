package com.ivanovsky.passnotes.domain.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.extensions.matches

class StrictEntryMatcher : EntryMatcher {

    override fun match(
        query: String,
        entries: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> {
        return entries.filter { entry ->
            when (entry) {
                is Note -> entry.matches(query)
                is Group -> entry.matches(query)
                else -> throw IllegalArgumentException()
            }
        }
    }
}