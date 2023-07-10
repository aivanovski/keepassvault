package com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResolveConflictDialogArgs(
    val file: FileDescriptor
) : Parcelable