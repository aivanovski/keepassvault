package com.ivanovsky.passnotes.presentation.core.model

class OptionPanelCellModel(
    override val id: String,
    val positiveText: String,
    val negativeText: String,
    val message: String,
    val isVisible: Boolean
) : BaseCellModel()