package com.ivanovsky.passnotes.data.entity

import java.util.Date
import java.util.UUID

data class Note(
    val uid: UUID? = null,
    val groupUid: UUID,
    val created: Date,
    val modified: Date,
    val expiration: Date?,
    val title: String,
    val properties: List<Property> = emptyList(),
    val attachments: List<Attachment> = emptyList()
) : EncryptedDatabaseEntry