package com.ivanovsky.passnotes.presentation.core.model

data class NavigationPanelCellModel(
    val items: List<String>,
    val isVisible: Boolean
) : BaseCellModel()