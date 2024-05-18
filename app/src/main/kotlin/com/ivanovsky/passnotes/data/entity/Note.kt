package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import java.util.Date
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note(
    val uid: UUID? = null,
    val groupUid: UUID,
    val created: Date,
    val modified: Date,
    val expiration: Date?,
    val title: String,
    val properties: List<Property> = emptyList(),
    val attachments: List<Attachment> = emptyList()
) : EncryptedDatabaseEntry, Parcelable