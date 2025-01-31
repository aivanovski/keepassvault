package com.ivanovsky.passnotes.injection.modules.debug

import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.test.usecases.DebugBiometricResolverImpl
import org.koin.dsl.module

object DebugBiometricModule {

    fun build() =
        module {
            single<BiometricResolver> { DebugBiometricResolverImpl(get(), get(), get()) }
        }
}