package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.biometric.FakeBiometricAuthenticatorImpl
import com.ivanovsky.passnotes.domain.biometric.FakeBiometricInteractorImpl
import org.koin.dsl.module

object FakeBiometricModule {

    fun build() =
        module {
            single<BiometricInteractor> { FakeBiometricInteractorImpl() }
            single<BiometricAuthenticator> { FakeBiometricAuthenticatorImpl(get()) }
        }
}