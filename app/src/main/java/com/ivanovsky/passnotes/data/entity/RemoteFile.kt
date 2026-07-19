package com.ivanovsky.passnotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_file")
data class RemoteFile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,

    @ColumnInfo(name = "fs_authority")
    val fsAuthority: FSAuthority,

    @ColumnInfo(name = "locally_modified")
    val isLocallyModified: Boolean = false,

    @ColumnInfo(name = "uploaded")
    val isUploaded: Boolean = false,

    @ColumnInfo(name = "upload_failed")
    val isUploadFailed: Boolean = false,

    @ColumnInfo(name = "uploading")
    val isUploading: Boolean = false,

    @ColumnInfo(name = "downloading")
    val isDownloading: Boolean = false,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_retry_timestamp")
    val lastRetryTimestamp: Long? = null,

    @ColumnInfo(name = "last_download_timestamp")
    val lastDownloadTimestamp: Long? = null,

    @ColumnInfo(name = "last_modification_timestamp")
    val lastModificationTimestamp: Long? = null,

    @ColumnInfo(name = "last_remote_modification_timestamp")
    val lastRemoteModificationTimestamp: Long? = null,

    @ColumnInfo(name = "local_path")
    val localPath: String,

    @ColumnInfo(name = "remote_path")
    val remotePath: String,

    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "revision")
    val revision: String? = null
)