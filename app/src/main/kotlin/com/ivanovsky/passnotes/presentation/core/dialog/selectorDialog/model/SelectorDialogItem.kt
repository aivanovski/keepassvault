package com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectorDialogItem(
    val title: String,
    val description: String
) : Parcelable