package com.ivanovsky.passnotes.presentation.service.task

import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.extensions.getLockServiceMessage
import com.ivanovsky.passnotes.presentation.service.LockServiceFacade

class ShowNotificationTask(
    private val interactor: LockServiceInteractor,
    private val resourceProvider: ResourceProvider
) : LockServiceTask {

    override suspend fun execute(service: LockServiceFacade) {
        val syncState = interactor.getLastSyncState() ?: SyncState.DEFAULT

        service.showNotification(syncState.getLockServiceMessage(resourceProvider))
    }
}