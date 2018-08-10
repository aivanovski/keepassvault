package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.repository.file.FSType

data class StorageOption(val type: FSType, val name: String)
