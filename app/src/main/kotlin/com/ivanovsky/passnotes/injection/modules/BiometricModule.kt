package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticatorImpl
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractorImpl
import org.koin.dsl.module

object BiometricModule {

    fun build() =
        module {
            single<BiometricInteractor> { BiometricInteractorImpl(get()) }
            single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
        }
}