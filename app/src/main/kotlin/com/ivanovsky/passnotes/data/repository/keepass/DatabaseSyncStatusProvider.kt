package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.encdb.DatabaseWatcher
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DatabaseSyncStatusProvider(
    private val fileSystemResolver: FileSystemResolver,
    dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus
) : DatabaseWatcher.OnCommitListener {

    val status: SyncStatus?
        get() = statusRef.get()

    private val statusRef = AtomicReference<SyncStatus?>()
    private val scope = CoroutineScope(dispatchers.IO)

    fun clear() {
        statusRef.set(null)
        Timber.d("status=null")
    }

    override fun onCommit(
        database: EncryptedDatabase,
        result: OperationResult<*>
    ) {
        val currentStatus = statusRef.get()
        when {
            currentStatus == SyncStatus.NO_CHANGES && result.isDeferred -> {
                setSyncStatus(SyncStatus.LOCAL_CHANGES, isNotify = true)
            }
        }

        scope.launch {
            val file = database.file
            val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

            val newStatus = fsProvider.syncProcessor.getSyncStatusForFile(file.uid)
            setSyncStatus(newStatus, isNotify = true)
        }
    }

    private fun setSyncStatus(
        newStatus: SyncStatus?,
        isNotify: Boolean
    ) {
        val prevStatus = statusRef.get()

        statusRef.set(newStatus)
        Timber.d("status=$newStatus")

        if (isNotify && prevStatus != newStatus) {
            observerBus.notifyDatabaseSyncStatusChanged(newStatus)
        }
    }
}