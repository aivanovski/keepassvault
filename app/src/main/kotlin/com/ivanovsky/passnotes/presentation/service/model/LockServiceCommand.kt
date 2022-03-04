package com.ivanovsky.passnotes.presentation.service.model

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import kotlinx.parcelize.Parcelize

sealed class LockServiceCommand : Parcelable {
    @Parcelize
    object ShowNotification : LockServiceCommand()

    @Parcelize
    object Stop : LockServiceCommand()

    @Parcelize
    data class SyncAndLock(val file: FileDescriptor) : LockServiceCommand()
}