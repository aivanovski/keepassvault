package com.ivanovsky.passnotes

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.injection.DIModuleBuilder
import com.ivanovsky.passnotes.injection.DefaultModuleBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

open class App : MultiDexApplication() {

    open fun configureModuleBuilder(builder: DIModuleBuilder) {
        // implementation should be flavor specific
    }

    override fun onCreate() {
        super.onCreate()

        val settings = SettingsImpl(context = this)
        val loggerInteractor = LoggerInteractor(context = this, settings)
            .apply {
                initialize()
            }

        val moduleBuilder = if (BuildConfig.DEBUG) {
            val type = Class.forName("com.ivanovsky.passnotes.injection.DebugModuleBuilder")

            val constructor = type.getConstructor(
                Context::class.java,
                LoggerInteractor::class.java,
                Settings::class.java
            )

            constructor.newInstance(this, loggerInteractor, settings) as DIModuleBuilder
        } else {
            DefaultModuleBuilder(loggerInteractor)
        }

        configureModuleBuilder(moduleBuilder)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(moduleBuilder.buildModules())
        }
    }
}