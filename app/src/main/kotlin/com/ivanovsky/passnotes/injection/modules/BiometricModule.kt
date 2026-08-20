package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.biometric.BiometricResolverImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

object BiometricModule {

    fun build() =
        module {
            singleOf(::BiometricResolverImpl).bind(BiometricResolver::class)
        }
}