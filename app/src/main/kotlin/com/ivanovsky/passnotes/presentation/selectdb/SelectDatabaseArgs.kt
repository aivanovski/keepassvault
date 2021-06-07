package com.ivanovsky.passnotes.presentation.selectdb

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SelectDatabaseArgs(
    val selectedFile: FileDescriptor
) : Parcelable