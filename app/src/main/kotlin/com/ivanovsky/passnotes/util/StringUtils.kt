package com.ivanovsky.passnotes.util

import java.lang.StringBuilder
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

object StringUtils {

    const val EMPTY = ""
    const val LINE_BREAK = "\n"
    const val DOT = '.'
    const val SPACE = ' '
    const val STAR = '*'
    const val URL = "URL"
    const val SLASH = '/'
    const val QUESTION_MARK = '?'
    const val COLON = ':'
    const val COLON_URL_ENCODED = "%3A"
    const val AMPERSAND = '&'
    const val EQUALS = '='

    private val UNITS = listOf("B", "KB", "MB", "GB", "TB")

    fun formatFileSize(size: Long): String {
        if (size <= 0) {
            return "0 B"
        }

        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#")
            .format(size / 1024.0.pow(digitGroups.toDouble()))
            .toString() + " " + UNITS[min(digitGroups, UNITS.lastIndex)]
    }

    fun splitIntoWords(text: String, wordLength: Int): String {
        return StringBuilder()
            .apply {
                var startIdx = 0
                while (startIdx < text.length) {
                    val endIdx = min(startIdx + wordLength, text.length)

                    append(text.substring(startIdx, endIdx))
                    if (endIdx != text.length) {
                        append(SPACE)
                    }

                    startIdx = endIdx
                }
            }
            .toString()
    }
}