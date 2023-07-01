package com.ivanovsky.passnotes.presentation.service.task

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.presentation.service.LockServiceFacade

class ShowNotificationTask(
    private val interactor: LockServiceInteractor,
    private val resourceProvider: ResourceProvider
) : LockServiceTask {

    override suspend fun execute(service: LockServiceFacade) {
        val getStatusResult = interactor.getDatabaseStatus()
        if (getStatusResult.isFailed) {
            return
        }

        val message = getMessageByDatabaseStatus(getStatusResult.getOrThrow())

        service.showNotification(message)
    }

    private fun getMessageByDatabaseStatus(status: DatabaseStatus): String {
        return if (status == DatabaseStatus.POSTPONED_CHANGES) {
            resourceProvider.getString(R.string.lock_notification_text_not_synchronized)
        } else {
            resourceProvider.getString(R.string.lock_notification_text_normal)
        }
    }
}