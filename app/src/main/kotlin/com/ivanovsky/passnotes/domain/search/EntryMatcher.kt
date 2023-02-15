package com.ivanovsky.passnotes.domain.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry

interface EntryMatcher {
    fun match(
        query: String,
        entries: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry>
}