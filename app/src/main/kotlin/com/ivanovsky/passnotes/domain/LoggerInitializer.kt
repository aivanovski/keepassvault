package com.ivanovsky.passnotes.domain

import android.content.Context
import android.util.Log
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.entity.LoggingType
import fr.bipi.tressence.base.PriorityTree
import fr.bipi.tressence.file.FileLoggerTree
import timber.log.Timber

class LoggerInitializer(private val context: Context) {

    private val settings = SettingsImpl(ResourceProvider(context), context)

    fun initialize() {
        Timber.uprootAll()

        when (settings.loggingType) {
            LoggingType.DEBUG -> {
                Timber.plant(Timber.DebugTree())
            }
            LoggingType.RELEASE -> {
                Timber.plant(PriorityTree(Log.INFO))
            }
            LoggingType.FILE -> {
                val dir = context.filesDir ?: throw IllegalStateException()
                Timber.plant(
                    Timber.DebugTree(),
                    FileLoggerTree.Builder()
                        .withDir(dir)
                        .withFileName(LOG_FILE_NAME)
                        .withSizeLimit(LOG_FILE_MAX_SIZE)
                        .withFileLimit(LOG_FILES_COUNT)
                        .build()
                )
            }
        }
    }

    companion object {
        private const val LOG_FILE_NAME = "log"
        private const val LOG_FILE_MAX_SIZE = 5 * 1024 * 1024
        private const val LOG_FILES_COUNT = 5
    }
}