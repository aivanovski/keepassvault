package com.ivanovsky.passnotes.extensions

import android.database.Cursor

fun Cursor.readRow(): Map<String, Any?> {
    moveToNext()

    val map = mutableMapOf<String, Any?>()

    for (columnIdx in 0 until columnCount) {
        val columnType = getType(columnIdx)
        val columnName = getColumnName(columnIdx)

        val columnValue = when (columnType) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(columnIdx)
            Cursor.FIELD_TYPE_FLOAT -> getFloat(columnIdx)
            Cursor.FIELD_TYPE_STRING -> getString(columnIdx)
            Cursor.FIELD_TYPE_NULL -> null
            else -> throw IllegalStateException()
        }

        map[columnName] = columnValue
    }

    return map
}