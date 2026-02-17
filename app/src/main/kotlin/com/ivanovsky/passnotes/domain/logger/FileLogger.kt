package com.ivanovsky.passnotes.domain.logger

import android.util.Log
import java.io.File
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface FileLogger {
    fun clear()
    fun log(level: Level, tag: String?, message: String, t: Throwable?)
}

fun interface TimeProvider {
    fun currentTimeMillis(): Long
}

interface SchedulerTimeProvider {
    fun currentTimeMillis(): Long
    fun calculateTimeSince(timestamp: Long): Long
}

enum class Level {
    VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT
}

class FileLoggerImpl(
    coroutineDispatcher: CoroutineDispatcher,
    private val fileSystem: FileSystemFacade,
    private val schedulerTimeProvider: SchedulerTimeProvider,
    private val logTimeProvider: TimeProvider,
    private val maxFileSizeInBytes: Long,
    private val maxFiles: Int,
    private val fileName: String,
    private val fileExtension: String = "log",
    private val isPrintInnerErrors: Boolean = true
) : FileLogger {

    private val logsDir = fileSystem.getRootDir()
    private val scope = CoroutineScope(coroutineDispatcher)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val channel = Channel<Message>(
        capacity = 10_000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val lastWriteTime = AtomicLong(0)
    private val isRunning = AtomicBoolean(true)
    private val isCleared = AtomicBoolean(false)
    private val fileIndex = AtomicInteger(0)
    private val currentFile = AtomicReference<File>()
    private val currentWriter = AtomicReference<Writer>()

    init {
        fileSystem.ensureDirectory(logsDir)
        fileIndex.set(findLastIndex())
        currentFile.set(getFile(fileIndex.get()))
        startCollector()
    }

    override fun log(
        level: Level,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        if (!isRunning.get() || isCleared.get()) return

        channel.trySend(
            Message.LogMessage(
                level,
                tag,
                message,
                t,
                logTimeProvider.currentTimeMillis()
            )
        )
    }

    override fun clear() {
        if (!isRunning.get()) return

        channel.trySend(Message.Stop)
    }

    private fun startCollector() {
        scope.launch {
            for (message in channel) {
                if (!isRunning.get()) continue

                when (message) {
                    is Message.LogMessage -> writeToFile(message)
                    is Message.Stop -> stop()
                    is Message.ReleaseResources -> releaseWriter()
                }
            }
        }

        scope.launch {
            do {
                delay(RELEASE_RESOURCES_DELAY_IN_MS)

                val lastWrite = lastWriteTime.get()
                if (lastWrite == 0L) continue

                val timeSinceLastWrite = schedulerTimeProvider.calculateTimeSince(lastWrite)
                if (timeSinceLastWrite >= RELEASE_RESOURCES_DELAY_IN_MS &&
                    currentWriter.get() != null
                ) {
                    channel.send(Message.ReleaseResources)
                }
            } while (isRunning.get())
        }
    }

    private fun stop() {
        if (!isRunning.get()) return

        isRunning.set(false)
        releaseResources()

        if (scope.isActive) {
            scope.cancel()
            channel.close()
        }
    }

    private fun releaseResources() {
        if (isCleared.compareAndSet(false, true)) {
            releaseWriter()
        }
    }

    private fun writeToFile(message: Message.LogMessage) {
        runCatching {
            val writer = setupWriter()

            val line = formatLine(message)
            writer.append(line)
            writer.flush()

            lastWriteTime.set(schedulerTimeProvider.currentTimeMillis())
        }.onFailure { error ->
            if (isPrintInnerErrors) error.printStackTrace()
            releaseWriter()
        }
    }

    private fun releaseWriter() {
        runCatching { currentWriter.getOrNull()?.close() }
            .onFailure { error -> if (isPrintInnerErrors) error.printStackTrace() }
        currentWriter.set(null)
    }

    private fun setupWriter(): Writer {
        val currFile = currentFile.getOrNull()
        val currWriter = currentWriter.getOrNull()

        val isNeedToRotate =
            (currFile != null && fileSystem.fileSize(currFile) >= maxFileSizeInBytes)

        val file = if (isNeedToRotate) {
            releaseWriter()
            rotateFile()
        } else {
            currFile ?: getFile(fileIndex.get())
        }

        val writer = if (isNeedToRotate || currWriter == null) {
            fileSystem.openWriter(file)
        } else {
            currWriter
        }

        currentFile.set(file)
        currentWriter.set(writer)

        return writer
    }

    private fun rotateFile(): File {
        val indices = getLogFileIndices()

        fileIndex.set((indices.maxOrNull() ?: 0) + 1)

        val fileToWrite = getFile(fileIndex.get())

        val indicesToRemove = indices.dropLast(maxFiles - 1)
        for (index in indicesToRemove) {
            fileSystem.delete(getFile(index))
        }

        return fileToWrite
    }

    private fun getFile(index: Int): File {
        return fileSystem.resolve("$fileName.$index.$fileExtension")
    }

    private fun findLastIndex(): Int {
        return getLogFileIndices().maxOrNull() ?: 0
    }

    private fun getLogFileIndices(): List<Int> {
        return getLogFiles()
            .mapNotNull { file ->
                file.name
                    .split(".")
                    .getOrNull(1)
                    ?.toIntOrNull()
            }
            .sorted()
    }

    private fun getLogFiles(): List<File> {
        return fileSystem.listFiles(logsDir).filter { file ->
            file.name.startsWith(fileName) && file.name.endsWith(fileExtension)
        }
    }

    private fun formatLine(message: Message.LogMessage): String {
        return buildString {
            append(dateFormat.format(Date(message.time)))
            append(" ")
            append(message.level.name)
            append("/")
            append(message.tag ?: "App")
            append(": ")
            append(message.text)

            if (message.throwable != null) {
                append("\n")
                append(Log.getStackTraceString(message.throwable))
            }

            append("\n")
        }
    }

    private fun <T> AtomicReference<T>.getOrNull(): T? = get()

    private sealed interface Message {

        data object Stop : Message

        data object ReleaseResources : Message

        data class LogMessage(
            val level: Level,
            val tag: String?,
            val text: String,
            val throwable: Throwable?,
            val time: Long
        ) : Message
    }

    companion object {
        const val RELEASE_RESOURCES_DELAY_IN_MS = 5000L
    }
}