package com.ivanovsky.passnotes

import android.app.Application
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.loggingAndReporting.CrashReporterInteractor
import com.ivanovsky.passnotes.domain.loggingAndReporting.LoggerInteractor
import com.ivanovsky.passnotes.injection.AppStartDependencies
import com.ivanovsky.passnotes.injection.DIModuleBuilder
import com.ivanovsky.passnotes.injection.DebugModuleBuilder
import com.ivanovsky.passnotes.injection.DefaultModuleBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

open class App : Application() {

    open fun configureModuleBuilder(builder: DIModuleBuilder) {
        // implementation should be flavor specific
        if (BuildConfig.IS_AUTOMATION_BUILD) {
            builder.isExternalStorageAccessEnabled = true
        }
    }

    override fun onCreate() {
        super.onCreate()

        val settings = SettingsImpl(context = this)

        val deps = AppStartDependencies(
            context = this,
            settings = settings,
            loggerInteractor = LoggerInteractor(context = this, settings)
                .apply {
                    initialize()
                },
            crashReporterInteractor = CrashReporterInteractor(context = this)
                .apply {
                    initialize(settings)
                }
        )

        val moduleBuilder = if (BuildConfig.DEBUG) {
            DebugModuleBuilder(deps)
        } else {
            DefaultModuleBuilder(deps)
        }

        configureModuleBuilder(moduleBuilder)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(moduleBuilder.buildModules())
        }
    }
}