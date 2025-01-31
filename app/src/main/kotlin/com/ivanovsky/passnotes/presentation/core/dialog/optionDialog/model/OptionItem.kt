package com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OptionItem(
    val title: String,
    val description: String?
) : Parcelable