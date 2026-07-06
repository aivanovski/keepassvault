package com.ivanovsky.passnotes

import android.app.Application
import android.content.Context
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.loggingAndReporting.CrashReporterInteractor
import com.ivanovsky.passnotes.domain.loggingAndReporting.LoggerInteractor
import com.ivanovsky.passnotes.injection.DIModuleBuilder
import com.ivanovsky.passnotes.injection.DefaultModuleBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

open class App : Application() {

    open fun configureModuleBuilder(builder: DIModuleBuilder) {
        // implementation should be flavor specific
    }

    override fun onCreate() {
        super.onCreate()

        val settings = SettingsImpl(context = this)
        val crashReporterInteractor = CrashReporterInteractor(context = this)
            .apply {
                initialize(settings)
            }

        val loggerInteractor = LoggerInteractor(context = this, settings)
            .apply {
                initialize()
            }

        val moduleBuilder = if (BuildConfig.DEBUG) {
            val type = Class.forName("com.ivanovsky.passnotes.injection.DebugModuleBuilder")

            val constructor = type.getConstructor(
                Context::class.java,
                LoggerInteractor::class.java,
                CrashReporterInteractor::class.java,
                Settings::class.java
            )

            constructor.newInstance(
                this,
                loggerInteractor,
                crashReporterInteractor,
                settings
            ) as DIModuleBuilder
        } else {
            DefaultModuleBuilder(loggerInteractor, crashReporterInteractor)
        }

        configureModuleBuilder(moduleBuilder)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(moduleBuilder.buildModules())
        }
    }
}