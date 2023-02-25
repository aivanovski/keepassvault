package com.ivanovsky.passnotes.data.entity

import java.util.Date

data class RemoteFileMetadata(
    val uid: String,
    val path: String,
    val serverModified: Date,
    val clientModified: Date,
    val revision: String
)