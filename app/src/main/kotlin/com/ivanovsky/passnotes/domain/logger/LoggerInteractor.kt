package com.ivanovsky.passnotes.domain.logger

import android.content.Context
import android.util.Log
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.entity.LoggingType
import fr.bipi.tressence.base.PriorityTree
import java.io.File
import timber.log.Timber

class LoggerInteractor(
    private val context: Context,
    private val settings: Settings
) {

    private val fileHelper = FileHelper(context, settings)

    fun initialize() {
        Timber.forest()
            .mapNotNull { logger -> logger as? FileLoggerTree }
            .forEach { fileLoggerTree -> fileLoggerTree.clear() }

        Timber.uprootAll()

        when (determineLoggingType()) {
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
                    FileLoggerTree(
                        maxFileSizeInBytes = LOG_FILE_MAX_SIZE.toLong(),
                        maxFiles = LOG_FILES_COUNT,
                        fileName = LOG_FILE_NAME,
                        logsDir = dir
                    )
                )
            }
        }
    }

    fun getActiveLogFile(): File? {
        return getAllLogFiles().maxByOrNull { file -> file.lastModified() }
    }

    fun removeLogFiles(): Boolean {
        val fileLogger = Timber.forest()
            .filterIsInstance<FileLoggerTree>()
            .firstOrNull()

        if (fileLogger != null) {
            Timber.uprootAll()
            fileLogger.clear()
        }

        val result = getFilesToDelete()
            .map { it.delete() }
            .all { it }

        if (fileLogger != null) {
            initialize()
        }

        return result
    }

    private fun getAllLogFiles(): List<File> {
        val files = fileHelper.filesDir?.listFiles()?.toList() ?: emptyList()
        return files.filter { file -> LOG_FILE_PATTERN.matcher(file.name).matches() }
    }

    private fun getFilesToDelete(): List<File> {
        val files = fileHelper.filesDir?.listFiles()?.toList() ?: emptyList()
        return files.filter { it.name.startsWith(LOG_FILE_NAME) }
    }

    private fun determineLoggingType(): LoggingType {
        return when {
            settings.isFileLogEnabled -> LoggingType.FILE
            !BuildConfig.DEBUG -> LoggingType.RELEASE
            else -> LoggingType.DEBUG
        }
    }

    companion object {
        private const val LOG_FILE_NAME = "keepassvault-log"
        private val LOG_FILE_PATTERN = "keepassvault-log(\\.[0-9])?\\.log".toPattern()
        private const val LOG_FILE_MAX_SIZE = 10 * 1024 * 1024
        private const val LOG_FILES_COUNT = 1
    }
}