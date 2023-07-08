package com.ivanovsky.passnotes.data.repository.encdb

import com.ivanovsky.passnotes.data.entity.OperationResult
import java.util.concurrent.CopyOnWriteArrayList

class DatabaseWatcher {

    private val listeners: MutableList<DatabaseListener> = CopyOnWriteArrayList()

    fun notifyOnCommit(
        database: EncryptedDatabase,
        result: OperationResult<*>
    ) {
        listeners.filterIsInstance<OnCommitListener>()
            .forEach { listener -> listener.onCommit(database, result) }
    }

    fun subscribe(listener: DatabaseListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unsubscribe(listener: DatabaseListener) {
        listeners.remove(listener)
    }

    interface DatabaseListener

    interface OnCommitListener : DatabaseListener {
        fun onCommit(
            database: EncryptedDatabase,
            result: OperationResult<*>
        )
    }
}