package com.ivanovsky.passnotes

import androidx.multidex.MultiDexApplication
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.injection.modules.BiometricModule
import com.ivanovsky.passnotes.injection.modules.CoreModule
import com.ivanovsky.passnotes.injection.modules.UiModule
import com.ivanovsky.passnotes.injection.modules.UseCaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module

open class App : MultiDexApplication() {

    protected open fun createKoinModules(
        loggerInteractor: LoggerInteractor,
        settings: Settings
    ): List<Module> {
        return listOf(
            CoreModule.build(loggerInteractor),
            BiometricModule.build(),
            UseCaseModule.build(),
            UiModule.build()
        )
    }

    override fun onCreate() {
        super.onCreate()

        val settings = SettingsImpl(context = this)
        val loggerInteractor = LoggerInteractor(context = this, settings)
            .apply {
                initialize()
            }

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(createKoinModules(loggerInteractor, settings))
        }
    }
}