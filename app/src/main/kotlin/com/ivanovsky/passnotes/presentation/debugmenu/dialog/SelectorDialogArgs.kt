package com.ivanovsky.passnotes.presentation.debugmenu.dialog

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.model.SelectorOption
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectorDialogArgs(
    val options: List<SelectorOption>,
    val selectedIndices: List<Int>
) : Parcelable