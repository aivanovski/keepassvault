package com.ivanovsky.passnotes.domain.workers

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
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import java.util.concurrent.TimeUnit
import timber.log.Timber

class BackgroundSyncWorker(
    content: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(content, workerParams) {

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

        fun schedule(settings: Settings, context: Context) {
            val interval = settings.backgroundSyncIntervalInMs

            Timber.d(
                "Schedule background worker[%s]: interval=%s".format(
                    BACKGROUND_SYNC_WORKER_NAME,
                    interval
                )
            )

            if (interval == -1) {
                WorkManager.getInstance(context).cancelUniqueWork(BACKGROUND_SYNC_WORKER_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkRequest(
                    NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                    NetworkType.CONNECTED
                )
                .build()

            val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                interval.toLong(),
                TimeUnit.MILLISECONDS
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