package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.storagelist.model.StorageOptionType

data class StorageOption(
    val type: StorageOptionType,
    val title: String,
    val root: FileDescriptor
)