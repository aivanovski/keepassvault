package com.ivanovsky.passnotes.presentation.service.task

import com.ivanovsky.passnotes.presentation.service.LockServiceFacade

class StopServiceTask : LockServiceTask {

    override suspend fun execute(service: LockServiceFacade) {
        service.stop()
    }
}