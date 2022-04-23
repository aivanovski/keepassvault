package com.ivanovsky.passnotes.domain.interactor.settings.database

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import kotlinx.coroutines.withContext

class DatabaseSettingsInteractor(
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getDbConfig(): OperationResult<EncryptedDatabaseConfig> {
        return withContext(dispatchers.IO) {
            val getDb = getDbUseCase.getDatabase()
            if (getDb.isFailed) {
                return@withContext getDb.takeError()
            }

            getDb.obj.config
        }
    }

    suspend fun applyDbConfig(config: EncryptedDatabaseConfig): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDb = getDbUseCase.getDatabase()
            if (getDb.isFailed) {
                return@withContext getDb.takeError()
            }

            getDb.obj.applyConfig(config)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            val getDb = getDbUseCase.getDatabase()
            if (getDb.isFailed) {
                return@withContext getDb.takeError()
            }

            getDb.obj.changeKey(
                PasswordKeepassKey(oldPassword),
                PasswordKeepassKey(newPassword)
            )
        }
    }
}