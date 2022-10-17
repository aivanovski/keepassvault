package com.ivanovsky.passnotes.presentation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.injection.GlobalInjector.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DatabaseLockBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val dispatchers: DispatcherProvider = get()
        val errorInteractor: ErrorInteractor = get()
        val lockDatabaseUseCase: LockDatabaseUseCase = get()

        CoroutineScope(dispatchers.IO).launch {
            Timber.d("Closing database")

            val lock = lockDatabaseUseCase.lockIfNeed()
            if (lock.isFailed) {
                val message = errorInteractor.processAndGetMessage(lock.error)
                Timber.d("Unable to close database: %s", message)
            }
        }
    }
}