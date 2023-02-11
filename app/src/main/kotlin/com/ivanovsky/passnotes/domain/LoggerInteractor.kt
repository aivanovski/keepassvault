package com.ivanovsky.passnotes.domain

import android.content.Context
import android.util.Log
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.entity.LoggingType
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.isDigitsOnly
import fr.bipi.tressence.base.PriorityTree
import fr.bipi.tressence.file.FileLoggerTree
import timber.log.Timber
import java.io.File

class LoggerInteractor(
    private val context: Context,
    private val settings: Settings
) {

    private val fileHelper = FileHelper(context, settings)

    fun initialize() {
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
                    FileLoggerTree.Builder()
                        .withDir(dir)
                        .withFileName(LOG_FILE_NAME)
                        .withSizeLimit(LOG_FILE_MAX_SIZE)
                        .withFileLimit(LOG_FILES_COUNT)
                        .withMinPriority(Log.VERBOSE)
                        .build()
                )
            }
        }
    }

    fun getActiveLogFile(): File? {
        return getAllLogFiles()
            .mapNotNull { file ->
                val logIndex = getLogFileIndex(file.name)

                if (logIndex != null) {
                    Pair(logIndex, file)
                } else {
                    null
                }
            }
            .minByOrNull { it.first }
            ?.second
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

    private fun getLogFileIndex(name: String): Int? {
        if (name == LOG_FILE_NAME) {
            return -1
        }

        val dotIdx = name.indexOf(StringUtils.DOT)
        if (dotIdx == -1) {
            return null
        }

        val logIndex = name.substring(dotIdx, name.length)
        if (!logIndex.isDigitsOnly()) {
            return null
        }

        return logIndex.toInt()
    }

    private fun determineLoggingType(): LoggingType {
        return when {
            settings.isFileLogEnabled -> LoggingType.FILE
            !BuildConfig.DEBUG -> LoggingType.RELEASE
            else -> LoggingType.DEBUG
        }
    }

    companion object {
        const val LOG_FILE_NAME = "log"
        private val LOG_FILE_PATTERN = "log(\\.[0-9]{1})?".toPattern()
        private const val LOG_FILE_MAX_SIZE = 5 * 1024 * 1024
        private const val LOG_FILES_COUNT = 1
    }
}