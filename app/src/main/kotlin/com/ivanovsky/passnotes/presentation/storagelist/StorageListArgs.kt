package com.ivanovsky.passnotes.presentation.storagelist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StorageListArgs(
    val action: Action
) : Parcelable