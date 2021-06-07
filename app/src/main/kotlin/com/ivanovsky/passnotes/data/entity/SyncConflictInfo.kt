package com.ivanovsky.passnotes.data.entity

data class SyncConflictInfo(
    val localFile: FileDescriptor,
    val remoteFile: FileDescriptor
)