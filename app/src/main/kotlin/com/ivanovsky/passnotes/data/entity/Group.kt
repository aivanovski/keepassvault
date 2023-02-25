package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

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