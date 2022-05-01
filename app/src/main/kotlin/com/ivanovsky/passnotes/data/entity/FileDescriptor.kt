package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileDescriptor(
    val fsAuthority: FSAuthority,
    val path: String,
    val uid: String, // TODO: it can be null at some situations
    val name: String,
    val isDirectory: Boolean,
    val isRoot: Boolean,
    val modified: Long? = null
) : Parcelable