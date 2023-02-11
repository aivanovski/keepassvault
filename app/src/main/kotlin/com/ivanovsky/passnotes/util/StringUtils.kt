package com.ivanovsky.passnotes.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

object StringUtils {

    const val EMPTY = ""
    const val DOT = '.'
    const val SPACE = ' '
    const val STAR = '*'

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
}