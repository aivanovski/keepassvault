package com.ivanovsky.passnotes.domain.logger

import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

class FileLoggerTree(
    maxFileSizeInBytes: Long,
    maxFiles: Int,
    fileName: String,
    logsDir: File
) : Timber.DebugTree() {

    private val fileSystem = FileSystemFacadeImpl(rootDir = logsDir)

    private val logger = FileLoggerImpl(
        coroutineDispatcher = Dispatchers.IO,
        fileSystem = fileSystem,
        schedulerTimeProvider = object : SchedulerTimeProvider {
            override fun currentTimeMillis(): Long = System.currentTimeMillis()

            override fun calculateTimeSince(timestamp: Long): Long =
                currentTimeMillis() - timestamp
        },
        logTimeProvider = { System.currentTimeMillis() },
        maxFileSizeInBytes = maxFileSizeInBytes,
        maxFiles = maxFiles,
        fileName = fileName
    )

    fun clear() {
        logger.clear()
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        val logTag = tag ?: Throwable().stackTrace
            .firstOrNull()
            ?.let { createStackElementTag(it) }

        val level = when (priority) {
            Log.VERBOSE -> Level.VERBOSE
            Log.DEBUG -> Level.DEBUG
            Log.INFO -> Level.INFO
            Log.WARN -> Level.WARN
            Log.ERROR -> Level.ERROR
            Log.ASSERT -> Level.ASSERT
            else -> Level.WARN
        }

        logger.log(level, logTag, message, t)
    }
}