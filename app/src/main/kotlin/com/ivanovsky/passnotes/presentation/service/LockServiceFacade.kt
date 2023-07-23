package com.ivanovsky.passnotes.presentation.service

interface LockServiceFacade {
    fun stop()
    fun showNotification(message: String)
    fun hideNotification()
}