package com.ivanovsky.passnotes.domain.interactor.passwordGenerator

import com.ivanovsky.passnotes.domain.entity.PasswordResource
import com.ivanovsky.passnotes.domain.usecases.GeneratePasswordUseCase

class PasswordGeneratorInteractor(
    private val useCase: GeneratePasswordUseCase
) {

    fun generatePassword(length: Int, resources: List<PasswordResource>): String =
        useCase.generate(length, resources)
}