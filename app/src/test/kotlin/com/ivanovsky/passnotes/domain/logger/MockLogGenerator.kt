package com.ivanovsky.passnotes.domain.logger

import com.ivanovsky.passnotes.domain.logger.FileLoggerImplTest.Companion.LOG_DATE_FORMAT
import com.ivanovsky.passnotes.domain.logger.FileLoggerImplTest.Companion.LOG_START_TIME
import com.ivanovsky.passnotes.domain.logger.FileLoggerImplTest.Companion.parseDateToMillis
import java.util.Date

class MockLogGenerator {

    private fun generateMessage(index: Int): MockLogMessage =
        MockLogMessage(
            index = index,
            level = Level.DEBUG,
            tag = "Tag $index",
            message = "Message $index",
            exception = null
        )

    fun generateLogLines(messageCount: Int): List<MockLogMessage> {
        return (0 until messageCount)
            .map { index -> generateMessage(index) }
    }

    fun generateLogLine(index: Int): String {
        val message = generateMessage(index)
        val timestamp = LOG_DATE_FORMAT.format(Date(parseDateToMillis(LOG_START_TIME) + index))
        return "%s %s/%s: %s"
            .format(
                timestamp,
                message.level.name,
                message.tag,
                message.message
            )
    }

    fun generateLogContent(linesCount: Int): String {
        return (0 until linesCount)
            .joinToString(
                separator = "\n",
                postfix = "\n",
                transform = { index -> generateMessage(index).formatToString() }
            )
    }

    fun generateLogContent(range: IntRange): String {
        return (0..range.last)
            .map { index -> generateMessage(index).formatToString() }
            .drop(range.first)
            .joinToString(
                separator = "\n",
                postfix = "\n"
            )
    }

    data class MockLogMessage(
        val index: Int,
        val level: Level,
        val tag: String,
        val message: String,
        val exception: Throwable?
    ) {

        fun formatToString(): String {
            val timestamp = LOG_DATE_FORMAT.format(Date(parseDateToMillis(LOG_START_TIME) + index))
            return "%s %s/%s: %s"
                .format(
                    timestamp,
                    level.name,
                    tag,
                    message
                )
        }
    }
}