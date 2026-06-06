package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import java.util.Date
import java.util.UUID
import kotlinx.parcelize.Parcelize

/**
 * Interface for [Group] and [Note]
 */
sealed interface EncryptedDatabaseEntry : EncryptedDatabaseElement

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

// TODO(Refactor): Remove groupCount and noteCount, it should be in standalone entity
@Parcelize
data class Group(
    val uid: UUID,
    val parentUid: UUID?,
    val title: String,
    val groupCount: Int,
    val noteCount: Int,
    val autotypeEnabled: InheritableBooleanOption,
    val searchEnabled: InheritableBooleanOption
) : EncryptedDatabaseEntry, Parcelable