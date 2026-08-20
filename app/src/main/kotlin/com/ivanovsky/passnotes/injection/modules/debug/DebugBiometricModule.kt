package com.ivanovsky.passnotes.injection.modules.debug

import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.test.usecases.DebugBiometricResolverImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

object DebugBiometricModule {

    fun build() =
        module {
            singleOf(::DebugBiometricResolverImpl).bind(BiometricResolver::class)
        }
}