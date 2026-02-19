package com.ivanovsky.passnotes.presentation.debugmenu.dialog.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectorOption(
    val title: String,
    val description: String
) : Parcelable