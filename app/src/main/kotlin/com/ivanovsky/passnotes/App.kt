package com.ivanovsky.passnotes

import android.app.Application
import android.content.Context
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.logger.LoggerInteractor
import com.ivanovsky.passnotes.injection.DIModuleBuilder
import com.ivanovsky.passnotes.injection.DefaultModuleBuilder
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

open class App : Application() {

    open fun configureModuleBuilder(builder: DIModuleBuilder) {
        // implementation should be flavor specific
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST

            dialog {
                title = getString(R.string.crash_report_dialog_title)
                text = getString(R.string.crash_report_dialog_message)
                positiveButtonText = getString(R.string.crash_report_dialog_send)
                negativeButtonText = getString(R.string.cancel)
                commentPrompt = getString(R.string.crash_report_dialog_comment)
            }

            mailSender {
                mailTo = getString(R.string.crash_report_email)
                reportAsFile = false
                subject = getString(R.string.crash_report_email_subject)
                body = getString(R.string.crash_report_email_body)
            }
        }
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
