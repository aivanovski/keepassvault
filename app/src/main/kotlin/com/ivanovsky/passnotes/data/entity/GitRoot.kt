package com.ivanovsky.passnotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "git_root")
data class GitRoot(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int? = null,

    @ColumnInfo(name = "fs_authority")
    val fsAuthority: FSAuthority,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "ssh_key_path")
    val sshKeyPath: String?,

    @Embedded(prefix = "ssh_key_file_")
    val sshKeyFile: FileId?
)