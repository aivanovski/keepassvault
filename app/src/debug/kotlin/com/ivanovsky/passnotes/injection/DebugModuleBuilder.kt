package com.ivanovsky.passnotes.injection

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.injection.modules.CoreModule
import com.ivanovsky.passnotes.injection.modules.UiModule
import com.ivanovsky.passnotes.injection.modules.UseCaseModule
import com.ivanovsky.passnotes.injection.modules.debug.DebugBiometricModule
import com.ivanovsky.passnotes.injection.modules.debug.DebugFileSystemProvidersModule
import com.ivanovsky.passnotes.injection.modules.debug.DebugModule
import org.koin.core.module.Module

// Is loaded via reflection in App.kt
class DebugModuleBuilder(
    private val startDeps: AppStartDependencies
) : DIModuleBuilder {

    override var isExternalStorageAccessEnabled: Boolean = false

    override fun buildModules(): List<Module> {
        val isFakeFileSystemEnabled = startDeps.settings.testToggles?.isFakeFileSystemEnabled
            ?: BuildConfig.IS_AUTOMATION_BUILD

        return listOf(
            CoreModule.build(startDeps),
            DebugModule.build(),
            DebugFileSystemProvidersModule.build(
                isExternalStorageAccessEnabled = isExternalStorageAccessEnabled,
                isFakeFileSystemEnabled = isFakeFileSystemEnabled
            ),
            DebugBiometricModule.build(),
            UseCaseModule.build(),
            UiModule.build()
        )
    }
}