package com.ivanovsky.passnotes.presentation.service.task

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.presentation.service.LockServiceFacade
import timber.log.Timber

class SyncAndLockTask(
    private val interactor: LockServiceInteractor,
    private val resourceProvider: ResourceProvider,
    private val file: FileDescriptor
) : LockServiceTask {

    override suspend fun execute(service: LockServiceFacade) {
        service.showNotification(
            resourceProvider.getString(R.string.lock_notification_text_synchronizing)
        )

        Timber.d("Synchronizing: %s", file)

        val syncResult = interactor.syncAndLockIfNeed(file)
        if (syncResult.isFailed) {
            service.hideNotification()
            return
        }

        Timber.d("syncResult=%s", syncResult)
    }
}