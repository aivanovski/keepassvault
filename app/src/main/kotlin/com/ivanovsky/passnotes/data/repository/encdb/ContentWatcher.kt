package com.ivanovsky.passnotes.data.repository.encdb

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import java.util.concurrent.CopyOnWriteArrayList

class ContentWatcher<T : EncryptedDatabaseEntry> {

    private val listeners: MutableList<EntryListener<T>> = CopyOnWriteArrayList()

    fun notifyEntryInserted(entry: T) {
        listeners.filterIsInstance<OnEntryCreateListener<T>>()
            .forEach { it.onEntryCreated(entry) }
    }

    fun notifyEntriesInserted(entries: List<T>) {
        listeners.filterIsInstance<OnEntryCreateListener<T>>()
            .forEach {
                for (entry in entries) {
                    it.onEntryCreated(entry)
                }
            }
    }

    fun notifyEntryRemoved(entry: T) {
        listeners.filterIsInstance<OnEntryRemoveListener<T>>()
            .forEach { it.onEntryRemoved(entry) }
    }

    fun notifyEntryChanged(oldEntry: T, newEntry: T) {
        listeners.filterIsInstance<OnEntryChangeListener<T>>()
            .forEach { it.onEntryChanged(oldEntry, newEntry) }
    }

    fun subscribe(listener: EntryListener<T>) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unsubscribe(listener: EntryListener<T>) {
        listeners.remove(listener)
    }

    interface EntryListener<T : EncryptedDatabaseEntry>

    interface OnEntryCreateListener<T : EncryptedDatabaseEntry> : EntryListener<T> {
        fun onEntryCreated(entry: T)
    }

    interface OnEntryRemoveListener<T : EncryptedDatabaseEntry> : EntryListener<T> {
        fun onEntryRemoved(entry: T)
    }

    interface OnEntryChangeListener<T : EncryptedDatabaseEntry> : EntryListener<T> {
        fun onEntryChanged(oldEntry: T, newEntry: T)
    }
}