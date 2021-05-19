package com.ivanovsky.passnotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "used_file")
data class UsedFile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int? = null,

    @ColumnInfo(name = "fs_authority")
    val fsAuthority: FSAuthority,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "file_uid")
    val fileUid: String,

    @ColumnInfo(name = "added_time")
    val addedTime: Long,

    @ColumnInfo(name = "last_access_time")
    val lastAccessTime: Long? = null
)