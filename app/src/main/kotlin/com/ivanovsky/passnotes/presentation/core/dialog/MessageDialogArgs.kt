package com.ivanovsky.passnotes.presentation.core.dialog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageDialogArgs(
    val isError: Boolean,
    val message: String
) : Parcelable