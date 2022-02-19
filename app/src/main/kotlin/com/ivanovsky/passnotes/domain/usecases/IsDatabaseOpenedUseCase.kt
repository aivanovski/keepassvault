package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository

class IsDatabaseOpenedUseCase(private val dbRepo: EncryptedDatabaseRepository) {

    fun isDatabaseOpened() = dbRepo.isOpened
}