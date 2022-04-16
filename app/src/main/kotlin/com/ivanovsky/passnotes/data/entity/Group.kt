package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Group(
    val uid: UUID,
    val parentUid: UUID?,
    val title: String,
    val groupCount: Int,
    val noteCount: Int
) : EncryptedDatabaseEntry, Parcelable