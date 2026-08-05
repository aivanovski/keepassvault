package com.ivanovsky.passnotes.domain.worker

import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import java.util.concurrent.TimeUnit
import timber.log.Timber

class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val syncUseCases: SyncUseCases by inject()

    override suspend fun doWork(): Result {
        Timber.d("Periodic sync worker started")

        return syncUseCases.syncChanges().fold(
            ifLeft = { error ->
                Timber.e("Sync failed: %s".format(error))
                Timber.e(error.throwable)

                Result.retry()
            },
            ifRight = {
                Timber.d("Sync finished successfully")

                Result.success()
            }
        )
    }

    companion object {
        private const val BACKGROUND_SYNC_WORKER_NAME = "background-sync-worker"

        fun schedule(settings: Settings) {
            val context: Context = GlobalInjector.get()
            val interval = settings.backgroundSyncIntervalInMs

            Timber.d(
                "Schedule background worker: interval=%s".format(
                    if (interval != -1) interval / 1000L else interval
                )
            )

            if (interval == -1) {
                WorkManager.getInstance(context).cancelUniqueWork(BACKGROUND_SYNC_WORKER_NAME)
                return
            }

            val intervalInMinutes = settings.backgroundSyncIntervalInMs / 1000L

            val constraints = Constraints.Builder()
                .setRequiredNetworkRequest(
                    NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                    NetworkType.CONNECTED
                )
                .build()

            val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                intervalInMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BACKGROUND_SYNC_WORKER_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}