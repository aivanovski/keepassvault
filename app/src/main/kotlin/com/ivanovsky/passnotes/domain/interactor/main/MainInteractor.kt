package com.ivanovsky.passnotes.domain.interactor.main

import com.ivanovsky.passnotes.domain.usecases.IsDatabaseOpenedUseCase

class MainInteractor(
    private val dbOpenedUseCase: IsDatabaseOpenedUseCase
) {

    fun isDatabaseOpened() = dbOpenedUseCase.isDatabaseOpened()
}