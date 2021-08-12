package com.ivanovsky.passnotes.extensions

import androidx.annotation.StringRes
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus

@StringRes
fun DatabaseStatus.getNameResId(): Int? {
    return when (this) {
        DatabaseStatus.CACHED -> R.string.status_offline_mode
        DatabaseStatus.READ_ONLY -> R.string.status_read_only_mode
        else -> null
    }
}