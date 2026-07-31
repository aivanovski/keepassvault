package com.ivanovsky.passnotes.util

object ObjectUtils {
    @JvmStatic
    fun isEquals(first: Any?, second: Any?): Boolean = first == second

    @JvmStatic
    fun isNotEquals(first: Any?, second: Any?): Boolean = first != second
}