package com.ivanovsky.passnotes.domain.interactor.settings.database

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.SetupRecycleBinUseCase
import com.ivanovsky.passnotes.extensions.toEither
import com.ivanovsky.passnotes.presentation.settings.database.model.DatabaseSettingsData
import kotlinx.coroutines.withContext

class DatabaseSettingsInteractor(
    private val getDbUseCase: GetDatabaseUseCase,
    private val setupRecycleBinUseCase: SetupRecycleBinUseCase,
    private val dispatchers: DispatcherProvider
) {

    suspend fun loadData(): Either<OperationError, DatabaseSettingsData> =
        withContext(dispatchers.IO) {
            either {
                val database = getDbUseCase.getDatabase().toEither().bind()

                val config = database.getConfig().bind()

                val groups = database.groupDao.all.toEither().bind()
                    .filter { group -> group.parentUid != null }

                DatabaseSettingsData(
                    config = config,
                    groups = groups
                )
            }
        }

    suspend fun applyDbConfig(
        config: EncryptedDatabaseConfig
    ): Either<OperationError, EncryptedDatabaseConfig> =
        withContext(dispatchers.IO) {
            either {
                val db = getDbUseCase.getDatabase().toEither().bind()

                db.applyConfig(config).bind()

                config
            }
        }

    suspend fun setupRecycleBinGroup(): Either<OperationError, Group> =
        setupRecycleBinUseCase.setupRecycleBinGroup()

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String
    ): Either<OperationError, Boolean> =
        withContext(dispatchers.IO) {
            either {
                val database = getDbUseCase.getDatabase().toEither().bind()

                database.changeKey(
                    PasswordKeepassKey(oldPassword),
                    PasswordKeepassKey(newPassword)
                ).bind()
            }
        }
}