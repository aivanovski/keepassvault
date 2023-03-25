package com.ivanovsky.passnotes.presentation.main.navigation.cells.model

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

data class NavigationHeaderCellModel(
    override val id: Int,
    val text: String
) : BaseCellModel(id)