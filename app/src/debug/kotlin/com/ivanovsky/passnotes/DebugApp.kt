package com.ivanovsky.passnotes

import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.injection.modules.BiometricModule
import com.ivanovsky.passnotes.injection.modules.CoreModule
import com.ivanovsky.passnotes.injection.modules.FakeBiometricModule
import com.ivanovsky.passnotes.injection.modules.UiModule
import com.ivanovsky.passnotes.injection.modules.UseCaseModule
import org.koin.core.module.Module

class DebugApp : App() {

    override fun createKoinModules(
        loggerInteractor: LoggerInteractor,
        settings: Settings
    ): List<Module> {
        val isLoadTestBiometricModule = settings.testToggles?.isFakeBiometricEnabled ?: false

        return listOf(
            CoreModule.build(loggerInteractor),
            if (isLoadTestBiometricModule) {
                FakeBiometricModule.build()
            } else {
                BiometricModule.build()
            },
            UseCaseModule.build(),
            UiModule.build()
        )
    }
}