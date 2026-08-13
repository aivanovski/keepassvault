package com.ivanovsky.passnotes.presentation.core.dialog.confirmationDialog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConfirmationDialogArgs(
    val actionId: Int,
    val message: String,
    val positiveButtonText: String,
    val negativeButtonText: String,
    val neutralButtonText: String? = null
) : Parcelable