package com.ivanovsky.passnotes.domain.interactor.settings.app

import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_REMOVE_FILE
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.RemoveBiometricDataUseCase
import kotlinx.coroutines.withContext
import java.io.File

class AppSettingsInteractor(
    private val loggerInteractor: LoggerInteractor,
    private val dispatchers: DispatcherProvider,
    private val lockUseCase: LockDatabaseUseCase,
    private val removeBiometricDataUseCase: RemoveBiometricDataUseCase
) {

    fun reInitializeLogging() {
        loggerInteractor.initialize()
    }

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }

    suspend fun getLogFile(): OperationResult<File> =
        withContext(dispatchers.IO) {
            val file = loggerInteractor.getActiveLogFile()

            if (file != null) {
                OperationResult.success(file)
            } else {
                OperationResult.error(newFileNotFoundError())
            }
        }

    suspend fun removeAllLogFiles(): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val removed = loggerInteractor.removeLogFiles()

            if (removed) {
                OperationResult.success(true)
            } else {
                OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_REMOVE_FILE))
            }
        }

    suspend fun removeAllBiometricData() =
        removeBiometricDataUseCase.removeAllBiometricData()
}