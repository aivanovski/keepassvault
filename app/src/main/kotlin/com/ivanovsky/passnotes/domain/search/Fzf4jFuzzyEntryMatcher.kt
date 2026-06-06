package com.ivanovsky.passnotes.domain.search

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.MatcherResult
import com.ivanovsky.passnotes.domain.entity.SearchOptions
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.isPropertySearchable
import de.gesundkrank.fzf4j.matchers.FuzzyMatcherV1
import de.gesundkrank.fzf4j.models.OrderBy
import java.util.UUID

class Fzf4jFuzzyEntryMatcher : EntryMatcher {

    override fun match(
        options: SearchOptions,
        query: String,
        entries: List<EncryptedDatabaseEntry>
    ): List<EncryptedDatabaseEntry> {
        val resultUids = mutableSetOf<UUID>()
        val result = mutableListOf<EncryptedDatabaseEntry>()

        val matchedEntriesByTitle = match(
            titles = entries.map { it.formatTitle(options) },
            entries = entries,
            query = query,
            isCaseSensitive = options.isCaseSensitive
        )
            .map { it.entry }

        for (entry in matchedEntriesByTitle) {
            val uid = entry.getUid() ?: continue
            result.add(entry)
            resultUids.add(uid)
        }

        val matchedEntriesByAllContent = match(
            titles = entries.map { it.formatAllContent(options) },
            entries = entries,
            query = query,
            isCaseSensitive = options.isCaseSensitive
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
        query: String,
        isCaseSensitive: Boolean
    ): List<MatcherResult<T>> {
        return FuzzyMatcherV1(titles, OrderBy.SCORE, false, isCaseSensitive)
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
        }
    }

    private fun EncryptedDatabaseEntry.formatTitle(options: SearchOptions): String {
        return when (this) {
            is Note -> if (options.isTitleEnabled) title else StringUtils.EMPTY
            is Group -> title
        }
    }

    private fun EncryptedDatabaseEntry.formatAllContent(options: SearchOptions): String {
        return when (this) {
            is Note -> formatAllContent(options)
            is Group -> title
        }
    }

    private fun Note.formatAllContent(options: SearchOptions): String {
        val words = mutableListOf<String>()

        for (property in properties) {
            if (options.isPropertySearchable(property)) {
                property.appendSearchableContentTo(words)
            }
        }

        if (options.isOtherFieldsEnabled) {
            for (attachment in attachments) {
                words.add(attachment.name)
            }
        }

        return words.joinToString(separator = " ")
    }

    private fun Property.appendSearchableContentTo(words: MutableList<String>) {
        val name = this.name
        val value = this.value
        val isDefaultProperty = PropertyType.DEFAULT_TYPES.contains(this.type)

        if (!isDefaultProperty && !name.isNullOrEmpty()) {
            words.add(name)
        }

        if (!value.isNullOrEmpty()) {
            words.add(value)
        }
    }
}