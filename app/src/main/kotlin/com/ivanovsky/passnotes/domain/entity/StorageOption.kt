package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.FileDescriptor

data class StorageOption(val type: StorageOptionType,
                         val title: String,
                         val root: FileDescriptor?)