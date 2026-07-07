package com.ivanovsky.passnotes.injection

import com.ivanovsky.passnotes.injection.modules.BiometricModule
import com.ivanovsky.passnotes.injection.modules.CoreModule
import com.ivanovsky.passnotes.injection.modules.FileSystemProvidersModule
import com.ivanovsky.passnotes.injection.modules.UiModule
import com.ivanovsky.passnotes.injection.modules.UseCaseModule
import org.koin.core.module.Module

class DefaultModuleBuilder(
    private val appDeps: AppStartDependencies
) : DIModuleBuilder {

    override var isExternalStorageAccessEnabled: Boolean = false

    override fun buildModules(): List<Module> {
        return listOf(
            CoreModule.build(appDeps),
            FileSystemProvidersModule.build(
                isExternalStorageAccessEnabled = isExternalStorageAccessEnabled
            ),
            BiometricModule.build(),
            UseCaseModule.build(),
            UiModule.build()
        )
    }
}