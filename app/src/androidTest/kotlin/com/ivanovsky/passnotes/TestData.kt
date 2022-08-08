package com.ivanovsky.passnotes

import java.util.Calendar

object TestData {

    const val DB_NAME = "test-db"

    val PLAIN_TEXT = """
        abcdefghijklmnopqrstuvwxyzABDCEFGHIJKLMNOPQRSTUVWXYZ0123456789
        ~`!@#$%^&*()_-+={[}]|\\:;\"'<,>.?/\t\b\n\r_
        """.trimIndent()

    fun dateInMillis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()

        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }
}