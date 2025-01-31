package com.ivanovsky.passnotes.presentation.core.dialog.optionDialog

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.model.OptionItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class OptionDialogArgs(
    val options: List<OptionItem>
) : Parcelable