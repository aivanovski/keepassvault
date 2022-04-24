package com.ivanovsky.passnotes.presentation.filepicker

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilePickerArgs(
    val action: Action,
    val rootFile: FileDescriptor,
    val isBrowsingEnabled: Boolean
) : Parcelable