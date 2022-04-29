package com.ivanovsky.passnotes.extensions

import java.lang.NumberFormatException

fun String.toIntSafely(): Int? {
    return try {
         Integer.parseInt(this)
    } catch (e: NumberFormatException) {
        null
    }
}