package com.ivanovsky.passnotes.presentation.core.dialog.sortAndView

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SortAndViewDialogArgs(
    val type: ScreenType
) : Parcelable