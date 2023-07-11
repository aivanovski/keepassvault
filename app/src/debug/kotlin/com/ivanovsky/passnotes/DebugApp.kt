package com.ivanovsky.passnotes

import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.injection.modules.BiometricModule
import com.ivanovsky.passnotes.injection.modules.CoreModule
import com.ivanovsky.passnotes.injection.modules.FakeBiometricModule
import com.ivanovsky.passnotes.injection.modules.FakeFileSystemProvidersModule
import com.ivanovsky.passnotes.injection.modules.FileSystemProvidersModule
import com.ivanovsky.passnotes.injection.modules.UiModule
import com.ivanovsky.passnotes.injection.modules.UseCaseModule
import org.koin.core.module.Module

class DebugApp : App() {

    override fun createKoinModules(
        loggerInteractor: LoggerInteractor,
        settings: Settings
    ): List<Module> {
        val isLoadFakeBiometricModule = settings.testToggles?.isFakeBiometricEnabled ?: false
        val isLoadFakeFileSystem = settings.testToggles?.isFakeFileSystemEnabled ?: false

        return listOf(
            CoreModule.build(loggerInteractor),
            if (isLoadFakeFileSystem) {
                FakeFileSystemProvidersModule.build(this)
            } else {
                FileSystemProvidersModule.build()
            },
            if (isLoadFakeBiometricModule) {
                FakeBiometricModule.build()
            } else {
                BiometricModule.build()
            },
            UseCaseModule.build(),
            UiModule.build()
        )
    }
}