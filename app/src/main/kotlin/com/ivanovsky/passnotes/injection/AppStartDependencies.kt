package com.ivanovsky.passnotes.injection

import android.content.Context
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.loggingAndReporting.CrashReporterInteractor
import com.ivanovsky.passnotes.domain.loggingAndReporting.LoggerInteractor

data class AppStartDependencies(
    val context: Context,
    val loggerInteractor: LoggerInteractor,
    val crashReporterInteractor: CrashReporterInteractor,
    val settings: Settings
)