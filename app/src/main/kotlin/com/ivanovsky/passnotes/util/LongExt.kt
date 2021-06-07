package com.ivanovsky.passnotes.util

fun Long?.isNewerThan(another: Long?): Boolean {
    return when {
        this == null -> false
        another == null -> true
        else -> this > another
    }
}