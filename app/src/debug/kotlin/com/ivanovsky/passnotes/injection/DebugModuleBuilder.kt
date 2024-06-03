package com.ivanovsky.passnotes.injection

import android.content.Context
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.injection.modules.BiometricModule
import com.ivanovsky.passnotes.injection.modules.CoreModule
import com.ivanovsky.passnotes.injection.modules.DebugFileSystemProvidersModule
import com.ivanovsky.passnotes.injection.modules.DebugModule
import com.ivanovsky.passnotes.injection.modules.FakeBiometricModule
import com.ivanovsky.passnotes.injection.modules.UiModule
import com.ivanovsky.passnotes.injection.modules.UseCaseModule
import org.koin.core.module.Module

// Is loaded via reflection in App.kt
class DebugModuleBuilder(
    private val context: Context,
    private val loggerInteractor: LoggerInteractor,
    private val settings: Settings
) : DIModuleBuilder {

    override var isExternalStorageAccessEnabled: Boolean = false

    override fun buildModules(): List<Module> {
        val isLoadFakeBiometricModule = settings.testToggles?.isFakeBiometricEnabled ?: false
        val isFakeFileSystemEnabled = settings.testToggles?.isFakeFileSystemEnabled ?: false

        return listOf(
            CoreModule.build(loggerInteractor),
            DebugModule.build(),
            DebugFileSystemProvidersModule.build(
                isExternalStorageAccessEnabled = isExternalStorageAccessEnabled,
                isFakeFileSystemEnabled = isFakeFileSystemEnabled
            ),
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