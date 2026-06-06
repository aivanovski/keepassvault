package com.ivanovsky.passnotes.domain.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.domain.entity.SearchOptions

interface EntryMatcher {
    fun match(
        options: SearchOptions,
        query: String,
        entries: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry>
}