package com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model

import androidx.compose.runtime.Immutable

@Immutable
data class SelectorDialogState(
    val title: String,
    val description: String?,
    val items: List<SelectorDialogItem>,
    val selectedItemIndex: Int?
)