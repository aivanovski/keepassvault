package com.ivanovsky.passnotes.domain.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.CleanUpFilesUseCase
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import timber.log.Timber

class CleanUpWorker(
    content: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(content, workerParams) {

    private val cleanUpUseCase: CleanUpFilesUseCase by inject()
    private val dispatchers: DispatcherProvider by inject()

    override suspend fun doWork(): Result =
        withContext(dispatchers.IO) {
            cleanUpUseCase.removeUnusedFiles().fold(
                ifLeft = { error ->
                    Timber.e("Clean up failed: %s".format(error))
                    Timber.e(error.throwable)
                    Result.success()
                },
                ifRight = {
                    Timber.d("Sync finished successfully")
                    Result.success()
                }
            )
        }

    companion object {
        private const val CLEAN_UP_WORKER_NAME = "background-cleanup-worker"

        fun schedule(context: Context) {
            Timber.d("Schedule background worker[%s]".format(CLEAN_UP_WORKER_NAME))

            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                CLEAN_UP_WORKER_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}