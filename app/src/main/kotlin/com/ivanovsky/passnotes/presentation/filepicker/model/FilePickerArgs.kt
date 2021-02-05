package com.ivanovsky.passnotes.presentation.filepicker.model

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.filepicker.Action
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FilePickerArgs(
    val action: Action,
    val rootFile: FileDescriptor,
    val isBrowsingEnabled: Boolean
) : Parcelable