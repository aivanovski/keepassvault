package com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model.SelectorDialogItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectorDialogArgs(
    val title: String,
    val description: String?,
    val items: List<SelectorDialogItem>,
    val selectedItemIndex: Int? = null
) : Parcelable