package com.ivanovsky.passnotes.data

import android.os.Handler
import android.os.Looper
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.util.ReflectionUtils
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class ObserverBus {
    interface Observer

    interface GroupDataSetObserver : Observer {
        fun onGroupDataSetChanged()
    }

    interface UsedFileDataSetObserver : Observer {
        fun onUsedFileDataSetChanged()
    }

    interface UsedFileContentObserver : Observer {
        fun onUsedFileContentChanged(usedFileId: Int)
    }

    interface NoteDataSetChanged : Observer {
        fun onNoteDataSetChanged(groupUid: UUID)
    }

    interface NoteContentObserver : Observer {
        fun onNoteContentChanged(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID)
    }

    interface DatabaseCloseObserver : Observer {
        fun onDatabaseClosed()
    }

    interface DatabaseOpenObserver : Observer {
        fun onDatabaseOpened(database: EncryptedDatabase)
    }

    interface DatabaseSyncStateObserver : Observer {
        fun onDatabaseSyncStateChanges(syncState: SyncState)
    }

    interface SyncProgressStatusObserver : Observer {
        fun onSyncProgressStatusChanged(
            fsAuthority: FSAuthority,
            uid: String,
            status: SyncProgressStatus
        )
    }

    /**
     * This observer is used to notify about changes in database data. It is used to update UI when
     * database data is changed.
     */
    interface DatabaseDataSetObserver : Observer {
        fun onDatabaseDataSetChanged()
    }

    private val observers = CopyOnWriteArrayList<Observer>()
    private val handler = Handler(Looper.getMainLooper())

    fun register(observer: Observer) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    fun unregister(observer: Observer) {
        if (observers.contains(observer)) {
            observers.remove(observer)
        }
    }

    fun notifyGroupDataSetChanged() {
        filterObservers(GroupDataSetObserver::class.java).forEach { observer ->
            handler.post(observer::onGroupDataSetChanged)
        }
    }

    fun notifyUsedFileDataSetChanged() {
        filterObservers(UsedFileDataSetObserver::class.java).forEach { observer ->
            handler.post(observer::onUsedFileDataSetChanged)
        }
    }

    fun notifyUsedFileContentChanged(usedFileId: Int) {
        filterObservers(UsedFileContentObserver::class.java).forEach { observer ->
            handler.post { observer.onUsedFileContentChanged(usedFileId) }
        }
    }

    fun notifyNoteDataSetChanged(groupUid: UUID) {
        filterObservers(NoteDataSetChanged::class.java).forEach { observer ->
            handler.post { observer.onNoteDataSetChanged(groupUid) }
        }
    }

    fun notifyNoteContentChanged(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID) {
        filterObservers(NoteContentObserver::class.java).forEach { observer ->
            handler.post { observer.onNoteContentChanged(groupUid, oldNoteUid, newNoteUid) }
        }
    }

    fun notifyDatabaseClosed() {
        filterObservers(DatabaseCloseObserver::class.java).forEach { observer ->
            handler.post(observer::onDatabaseClosed)
        }
    }

    fun notifyDatabaseOpened(database: EncryptedDatabase) {
        filterObservers(DatabaseOpenObserver::class.java).forEach { observer ->
            handler.post { observer.onDatabaseOpened(database) }
        }
    }

    fun notifySyncProgressStatusChanged(
        fsAuthority: FSAuthority,
        uid: String,
        status: SyncProgressStatus
    ) {
        filterObservers(SyncProgressStatusObserver::class.java).forEach { observer ->
            handler.post { observer.onSyncProgressStatusChanged(fsAuthority, uid, status) }
        }
    }

    fun notifyDatabaseDataSetChanged() {
        filterObservers(DatabaseDataSetObserver::class.java).forEach { observer ->
            handler.post(observer::onDatabaseDataSetChanged)
        }
    }

    fun notifyDatabaseSyncStateChanged(syncState: SyncState?) {
        if (syncState == null) return

        filterObservers(DatabaseSyncStateObserver::class.java).forEach { observer ->
            handler.post { observer.onDatabaseSyncStateChanges(syncState) }
        }
    }

    fun <T : Observer> hasObserver(type: Class<T>): Boolean = filterObservers(type).isNotEmpty()

    @Suppress("UNCHECKED_CAST")
    private fun <T> filterObservers(type: Class<T>): List<T> {
        return observers
            .filter { observer ->
                ReflectionUtils.containsInterfaceInClass(observer.javaClass, type)
            }.map { observer -> observer as T }
    }
}