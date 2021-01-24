package com.ivanovsky.passnotes.presentation.storagelist.model

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.storagelist.Action

class FilePickerArgs(
    val root: FileDescriptor,
    val action: Action,
    val isBrowsingEnabled: Boolean
)