package com.ivanovsky.passnotes.domain.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.entity.MatcherResult
import de.gesundkrank.fzf4j.matchers.FuzzyMatcherV1
import de.gesundkrank.fzf4j.models.OrderBy
import java.util.UUID

class Fzf4jFuzzyEntryMatcher : EntryMatcher {

    override fun match(
        query: String,
        entries: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> {
        val resultUids = mutableSetOf<UUID>()
        val result = mutableListOf<EncryptedDatabaseEntry>()

        val matchedEntriesByTitle = match(
            titles = entries.map { it.formatTitle() },
            entries = entries,
            query = query
        )
            .map { it.entry }

        for (entry in matchedEntriesByTitle) {
            val uid = entry.getUid() ?: continue
            result.add(entry)
            resultUids.add(uid)
        }

        val matchedEntriesByAllContent = match(
            titles = entries.map { it.formatAllContent() },
            entries = entries,
            query = query
        )
            .map { it.entry }

        for (entry in matchedEntriesByAllContent) {
            val uid = entry.getUid() ?: continue
            if (uid !in resultUids) {
                result.add(entry)
                resultUids.add(uid)
            }
        }

        return result
    }

    private fun <T> match(
        titles: List<String>,
        entries: List<T>,
        query: String
    ): List<MatcherResult<T>> {
        return FuzzyMatcherV1(titles, OrderBy.SCORE, false, false)
            .match(query)
            .map { result ->
                MatcherResult(
                    entry = entries[result.itemIndex],
                    title = titles[result.itemIndex],
                    highlights = result.positions?.toList() ?: emptyList()
                )
            }
    }

    private fun EncryptedDatabaseEntry.getUid(): UUID? {
        return when (this) {
            is Note -> uid
            is Group -> uid
            else -> null
        }
    }

    private fun EncryptedDatabaseEntry.formatTitle(): String {
        return when (this) {
            is Note -> title
            is Group -> title
            else -> ""
        }
    }

    private fun EncryptedDatabaseEntry.formatAllContent(): String {
        return when (this) {
            is Note -> {
                val words = mutableListOf<String>()

                for (property in properties) {
                    if (!property.name.isNullOrEmpty()) {
                        words.add(property.name)
                    }
                    if (!property.value.isNullOrEmpty()) {
                        words.add(property.value)
                    }
                }

                words.joinToString(separator = " ")
            }
            is Group -> title
            else -> ""
        }
    }
}