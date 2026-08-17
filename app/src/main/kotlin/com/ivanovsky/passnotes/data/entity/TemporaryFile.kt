package com.ivanovsky.passnotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temporary_file")
data class TemporaryFile(
    @PrimaryKey
    @ColumnInfo(name = "path")
    val path: String,
    val created: Long,
    val modified: Long?
)