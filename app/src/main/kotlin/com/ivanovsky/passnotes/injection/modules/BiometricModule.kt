package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.biometric.BiometricResolverImpl
import org.koin.dsl.module

object BiometricModule {

    fun build() =
        module {
            single<BiometricResolver> { BiometricResolverImpl(get()) }
        }
}