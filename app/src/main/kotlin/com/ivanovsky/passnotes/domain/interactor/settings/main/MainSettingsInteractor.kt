package com.ivanovsky.passnotes.domain.interactor.settings.main

import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository

class MainSettingsInteractor(private val dbRepo: EncryptedDatabaseRepository) {

    fun isDatabaseOpened(): Boolean {
        return dbRepo.isOpened
    }
}