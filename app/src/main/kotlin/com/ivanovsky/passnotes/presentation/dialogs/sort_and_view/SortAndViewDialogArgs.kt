package com.ivanovsky.passnotes.presentation.dialogs.sort_and_view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SortAndViewDialogArgs(
    val type: ScreenType
) : Parcelable