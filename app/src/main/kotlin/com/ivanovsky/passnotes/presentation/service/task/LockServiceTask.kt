package com.ivanovsky.passnotes.presentation.service.task

import com.ivanovsky.passnotes.presentation.service.LockServiceFacade

interface LockServiceTask {
    suspend fun execute(service: LockServiceFacade)
}