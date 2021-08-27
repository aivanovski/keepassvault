package com.ivanovsky.passnotes.presentation.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.interactor.settings.main.MainSettingsInteractor

class MainSettingsViewModel(
    private val interactor: MainSettingsInteractor,
    private val observerBus: ObserverBus
) : ViewModel(),
    ObserverBus.DatabaseOpenObserver,
    ObserverBus.DatabaseCloseObserver {

    val isDatabaseOpened = MutableLiveData(false)

    init {
        observerBus.register(this)
    }

    override fun onDatabaseClosed() {
        isDatabaseOpened.value = false
    }

    override fun onDatabaseOpened(fsOptions: FSOptions, status: DatabaseStatus) {
        isDatabaseOpened.value = true
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
    }

    fun start() {
        isDatabaseOpened.value = interactor.isDatabaseOpened()
    }
}