package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Used as @Embedded in database
@Parcelize
data class FileId(
    val fsAuthority: FSAuthority,
    val path: String,
    val uid: String,
    val name: String
) : Parcelable