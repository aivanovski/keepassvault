package com.ivanovsky.passnotes.domain.logger

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.domain.logger.FileLoggerImpl.Companion.RELEASE_RESOURCES_DELAY_IN_MS
import com.ivanovsky.passnotes.domain.logger.MockFileSystemFacade.MockWriter
import com.ivanovsky.passnotes.domain.logger.MockFileSystemFacade.ThrowOnWriteWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FileLoggerImplTest {

    @get:Rule
    var folder = TemporaryFolder()

    private val generator = MockLogGenerator()

    @Test
    fun `should write logs into new file`() {
        // arrange
        val logFile = newLogFile()
        assertThat(logFile.exists()).isFalse()

        // act
        val logger = newLogger()
        logger.writeLines(LOG_LINES)

        // assert
        assertThat(logFile.readText()).isEqualTo(LOG_LINES.formatToString())
    }

    @Test
    fun `should append logs to existing file`() {
        // arrange
        val messagesPerLogger = 3
        val totalMessages = messagesPerLogger + messagesPerLogger
        val logFile = newLogFile()

        // act
        val firstLogger = newLogger()
        firstLogger.writeLines(generator.generateLogLines(messagesPerLogger))
        assertThat(logFile.readText()).isEqualTo(generator.generateLogContent(messagesPerLogger))
        firstLogger.clear()

        val timestampProvider = CountingTimeProvider(messagesPerLogger.toLong())
        val secondLogger = newLogger(logTimeProvider = timestampProvider)
        secondLogger.writeLines(generator.generateLogLines(totalMessages).drop(messagesPerLogger))

        // assert
        assertThat(logFile.readText()).isEqualTo(generator.generateLogContent(totalMessages))
    }

    @Test
    fun `should rotate log files when max size reached`() {
        // arrange
        val linesInFirstFile = 100
        val totalLines = 120
        val maxFileSize = generator.generateLogContent(linesInFirstFile).length
        val logFile1 = newLogFile(fileIndex = 0)
        val logFile2 = newLogFile(fileIndex = 1)

        // act
        val logger = newLogger(maxFileSizeInBytes = maxFileSize.toLong(), maxFiles = 2)
        logger.writeLines(generator.generateLogLines(totalLines))

        // assert
        assertThat(logFile1.readText()).isEqualTo(generator.generateLogContent(linesInFirstFile))
        assertThat(logFile2.readText()).isEqualTo(
            generator.generateLogContent(linesInFirstFile until totalLines)
        )
    }

    @Test
    fun `clear should release resources`() {
        // arrange
        val logFile = newLogFile()
        val fileSystem = MockFileSystemFacade(folder.root)

        // act
        val logger = newLogger(fileSystem = fileSystem)
        logger.writeLines(LOG_LINES)
        logger.clear()

        // assert
        assertThat((fileSystem.lastWriter as MockWriter).isFlushed).isTrue()
        assertThat((fileSystem.lastWriter as MockWriter).isClosed).isTrue()
        assertThat(logFile.readText()).isEqualTo(LOG_LINES.formatToString())
    }

    @Test
    fun `should release writer and re-create it after idling`() = runTest {
        // arrange
        val lines = 5
        val totalLines = lines + lines
        val firstPartOfLogs = generator.generateLogLines(lines)
        val secondPartOfLogs = generator.generateLogLines(totalLines).drop(lines)

        val coroutineScheduler = TestCoroutineScheduler()
        val logFile = newLogFile()
        val dispatcher = UnconfinedTestDispatcher(coroutineScheduler)
        val schedulerTimeProvider = MockSchedulerTimeProvider()
        val fileSystem = MockFileSystemFacade(folder.root)

        // act
        val logger = newLogger(
            dispatcher = dispatcher,
            fileSystem = fileSystem,
            schedulerTimeProvider = schedulerTimeProvider
        )

        // Write first portion of logs and skip time until writer is released
        logger.writeLines(firstPartOfLogs)
        coroutineScheduler.advanceTimeBy(RELEASE_RESOURCES_DELAY_IN_MS + 100L)
        assertThat((fileSystem.lastWriter as MockWriter).isClosed).isTrue()

        // Write second portion of logs
        logger.writeLines(secondPartOfLogs)
        assertThat((fileSystem.lastWriter as MockWriter).isClosed).isFalse()

        // Stop logger loop
        logger.clear()

        // assert
        assertThat(logFile.readText()).isEqualTo(generator.generateLogContent(totalLines))
    }

    @Test
    fun `should handle IOException during writing`() {
        // arrange
        val logFile = newLogFile()
        val fileSystem = MockFileSystemFacade(
            rootDir = folder.root,
            writers = mapOf(logFile to ThrowOnWriteWriter(file = logFile))
        )

        // act
        val logger = newLogger(fileSystem = fileSystem)
        logger.writeLines(LOG_LINES)

        // assert
        assertThat(logFile.exists()).isFalse()
    }

    @Test
    fun `should restore after IOException during write`() {
        // arrange
        val logFile = newLogFile()
        val fileSystem = MockFileSystemFacade(
            rootDir = folder.root,
            writers = mapOf(logFile to ThrowOnWriteWriter(file = logFile, failedWrites = 2))
        )

        // act
        val logger = newLogger(fileSystem = fileSystem)
        logger.writeLines(LOG_LINES)

        // assert
        assertThat(logFile.readText()).isEqualTo(LOG_LINES.drop(2).formatToString())
    }

    @Test
    fun `should handle file deletion during writes`() {
        // arrange
        val skippedLines = 2
        val logFile = newLogFile()

        // act
        val logger = newLogger()
        logger.writeLines(LOG_LINES.take(skippedLines))

        // delete file
        logFile.delete()
        assertThat(logFile.exists()).isFalse()

        // write again
        logger.writeLines(LOG_LINES.drop(skippedLines))

        // assert
        assertThat(logFile.readText()).isEqualTo(LOG_LINES.drop(skippedLines).formatToString())
    }

    private fun FileLogger.writeLines(lines: List<MockLogGenerator.MockLogMessage>) {
        lines.forEach {
            log(it.level, it.tag, it.message, it.exception)
        }
    }

    private fun newLogger(
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
        fileSystem: FileSystemFacade = FileSystemFacadeImpl(folder.root),
        schedulerTimeProvider: SchedulerTimeProvider = MockSchedulerTimeProvider(),
        logTimeProvider: TimeProvider = CountingTimeProvider(0),
        maxFileSizeInBytes: Long = 1024L * 1024L,
        maxFiles: Int = 1
    ): FileLoggerImpl {
        return FileLoggerImpl(
            coroutineDispatcher = dispatcher,
            fileSystem = fileSystem,
            schedulerTimeProvider = schedulerTimeProvider,
            logTimeProvider = logTimeProvider,
            maxFileSizeInBytes = maxFileSizeInBytes,
            maxFiles = maxFiles,
            fileName = LOG_FILE_NAME,
            isPrintInnerErrors = false
        )
    }

    private fun newLogFile(fileIndex: Int = 0): File =
        File(folder.root, "$LOG_FILE_NAME.$fileIndex.log")

    private fun List<MockLogGenerator.MockLogMessage>.formatToString(): String {
        return this
            .joinToString(
                separator = "\n",
                postfix = "\n",
                transform = { message -> message.formatToString() }
            )
    }

    private class MockSchedulerTimeProvider : SchedulerTimeProvider {
        override fun currentTimeMillis(): Long =
            System.currentTimeMillis()

        override fun calculateTimeSince(timestamp: Long): Long =
            5000
    }

    private class CountingTimeProvider(
        initValue: Long
    ) : TimeProvider {

        private var time = parseDateToMillis(LOG_START_TIME) + initValue

        override fun currentTimeMillis(): Long {
            return time++
        }
    }

    companion object {

        val LOG_FILE_NAME = "test-log-file"
        val LOG_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val LOG_START_TIME = "2020-01-01 12:00:00.000"

        private val LOG_LINES = MockLogGenerator().generateLogLines(5)

        fun parseDateToMillis(date: String): Long {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            return df.parse(date)?.time
                ?: throw IllegalArgumentException("Invalid date string: $date")
        }
    }
}