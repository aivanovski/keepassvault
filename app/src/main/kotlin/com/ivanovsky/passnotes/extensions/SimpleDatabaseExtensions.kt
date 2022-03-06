package com.ivanovsky.passnotes.extensions

import org.linguafranca.pwdb.Entry
import org.linguafranca.pwdb.kdbx.simple.SimpleEntry
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup
import java.util.UUID

fun SimpleGroup.findEntries(
    isRecursive: Boolean,
    matcher: Entry.Matcher
): List<SimpleEntry> {
    val result = mutableListOf<SimpleEntry>()

    for (entry in entries) {
        if (matcher.matches(entry)) {
            result.add(entry)
        }
    }

    if (isRecursive) {
        for (group in groups) {
            result.addAll(group.findEntries(isRecursive, matcher))
        }
    }

    return result
}

fun SimpleGroup.findEntryByUid(
    isRecursive: Boolean,
    uid: UUID
): SimpleEntry? {
    for (entry in entries) {
        if (uid == entry.uuid) {
            return entry
        }
    }

    if (isRecursive) {
        for (group in groups) {
            val entry = group.findEntryByUid(isRecursive, uid)
            if (entry != null) {
                return entry
            }
        }
    }

    return null
}