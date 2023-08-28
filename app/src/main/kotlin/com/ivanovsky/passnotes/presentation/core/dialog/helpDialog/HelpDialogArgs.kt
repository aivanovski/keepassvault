package com.ivanovsky.passnotes.presentation.core.dialog.helpDialog

import android.os.Parcelable
import androidx.annotation.LayoutRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class HelpDialogArgs(
    val title: String?,
    @LayoutRes
    val layoutId: Int
) : Parcelable