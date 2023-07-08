package com.ivanovsky.passnotes.presentation.service.task

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.presentation.service.LockServiceFacade

class ShowNotificationTask(
    private val interactor: LockServiceInteractor,
    private val resourceProvider: ResourceProvider
) : LockServiceTask {

    override suspend fun execute(service: LockServiceFacade) {
        val getStatusResult = interactor.getLastSyncStatus()
        if (getStatusResult.isFailed) {
            return
        }

        val status = getStatusResult.getOrThrow() ?: return
        val message = getMessageBySyncStatus(status)

        service.showNotification(message)
    }

    private fun getMessageBySyncStatus(status: SyncStatus): String {
        return if (status == SyncStatus.NO_CHANGES) {
            resourceProvider.getString(R.string.lock_notification_text_normal)
        } else {
            resourceProvider.getString(R.string.lock_notification_text_not_synchronized)
        }
    }
}