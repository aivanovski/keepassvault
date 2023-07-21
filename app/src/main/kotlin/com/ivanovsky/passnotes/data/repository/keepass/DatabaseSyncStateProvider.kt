package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.encdb.DatabaseWatcher
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DatabaseSyncStateProvider(
    private val fileSystemResolver: FileSystemResolver,
    dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus
) : DatabaseWatcher.OnCommitListener,
    ObserverBus.SyncProgressStatusObserver {

    val syncState: SyncState?
        get() = syncStateRef.get()

    private val syncStateRef = AtomicReference<SyncState?>()
    private val dbFileRef = AtomicReference<FileDescriptor?>()
    private val scope = CoroutineScope(dispatchers.IO)

    init {
        observerBus.register(this)
    }

    override fun onCommit(
        database: EncryptedDatabase,
        result: OperationResult<*>
    ) {
        val currentState = syncStateRef.get()
        if (currentState?.status == SyncStatus.NO_CHANGES && result.isDeferred) {
            val newState = setupNewState(status = SyncStatus.LOCAL_CHANGES)
            setSyncState(newState, isNotify = true)
        }

        checkState()
    }

    override fun onSyncProgressStatusChanged(
        fsAuthority: FSAuthority,
        uid: String,
        progress: SyncProgressStatus
    ) {
        val file = dbFileRef.get() ?: return
        val currentProgress = syncStateRef.get()?.progress

        if (file.uid != uid || file.fsAuthority != fsAuthority) {
            return
        }

        if (progress == SyncProgressStatus.IDLE) {
            checkState()
        } else if (currentProgress != progress) {
            val newState = setupNewState(progress = progress)
            setSyncState(newState, isNotify = true)
        }
    }

    fun onDatabaseOpened(
        db: EncryptedDatabase,
        openResult: OperationResult<EncryptedDatabase>
    ) {
        syncStateRef.set(null)
        dbFileRef.set(db.file)

        onCommit(db, openResult)
    }

    fun onDatabaseClosed() {
        Timber.d("onDatabaseClosed")
        syncStateRef.set(null)
        dbFileRef.set(null)
    }

    private fun checkState() {
        val file = dbFileRef.get() ?: return

        scope.launch {
            val syncProcessor = fileSystemResolver.resolveSyncProcessor(file.fsAuthority)

            val status = syncProcessor.getSyncStatusForFile(file.uid)
            val progress = syncProcessor.getSyncProgressStatusForFile(file.uid)
            val revision = syncProcessor.getRevision(file.uid)

            val syncState = SyncState(status, progress, revision)

            setSyncState(syncState, isNotify = true)
        }
    }

    private fun setSyncState(
        newState: SyncState?,
        isNotify: Boolean
    ) {
        val prevState = syncStateRef.get()

        syncStateRef.set(newState)
        Timber.d("syncState=$newState")

        if (isNotify && prevState != newState) {
            observerBus.notifyDatabaseSyncStateChanged(newState)
        }
    }

    private fun setupNewState(
        status: SyncStatus? = null,
        progress: SyncProgressStatus? = null
    ): SyncState {
        val current = syncStateRef.get()

        return if (current != null) {
            current.copy(
                status = status ?: current.status,
                progress = progress ?: current.progress
            )
        } else {
            SyncState(
                status = status ?: SyncStatus.NO_CHANGES,
                progress = progress ?: SyncProgressStatus.IDLE,
                revision = null
            )
        }
    }
}