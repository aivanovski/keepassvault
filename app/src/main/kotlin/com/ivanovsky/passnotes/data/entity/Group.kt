package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

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