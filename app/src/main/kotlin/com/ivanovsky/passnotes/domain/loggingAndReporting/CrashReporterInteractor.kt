package com.ivanovsky.passnotes.domain.loggingAndReporting

import android.content.Context
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.repository.settings.Settings
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

class CrashReporterInteractor(
    private val context: Context
) {

    @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
    fun getAvailability(): CrashReporterAvailability {
        return when {
            BuildConfig.FLAVOR == FLAVOR_FDROID && !BuildConfig.DEBUG ->
                CrashReporterAvailability.UNAVAILABLE

            BuildConfig.SENTRY_DSN.isNotBlank() ->
                CrashReporterAvailability.AVAILABLE

            else -> CrashReporterAvailability.NOT_CONFIGURED
        }
    }

    fun initialize(settings: Settings) {
        setEnabled(settings.isCrashReportingEnabled)
    }

    fun setEnabled(isEnabled: Boolean) {
        if (!isEnabled || getAvailability() != CrashReporterAvailability.AVAILABLE) {
            Sentry.close()
            return
        }

        if (Sentry.isEnabled()) {
            return
        }

        SentryAndroid.init(context.applicationContext) { options ->
            options.setDsn(BuildConfig.SENTRY_DSN)
            options.dist = BuildConfig.VERSION_CODE.toString()
            options.environment = "${BuildConfig.FLAVOR}-${BuildConfig.BUILD_TYPE}"
            options.release = buildReleaseName()
        }
    }

    private fun buildReleaseName(): String =
        buildString {
            append(BuildConfig.APPLICATION_ID)
            append('@')
            append(BuildConfig.VERSION_NAME)
            append('+')
            append(BuildConfig.VERSION_CODE)
        }

    enum class CrashReporterAvailability {
        AVAILABLE,
        NOT_CONFIGURED,
        UNAVAILABLE
    }

    companion object {
        private const val FLAVOR_FDROID = "fdroid"
    }
}