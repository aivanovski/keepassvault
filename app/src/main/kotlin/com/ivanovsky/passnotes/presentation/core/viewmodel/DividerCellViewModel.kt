package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel

class DividerCellViewModel(
    override val model: DividerCellModel,
    resourceProvider: ResourceProvider
) : BaseCellViewModel(model) {

    val paddingStart: Int = if (model.paddingStart != null) {
        resourceProvider.getDimension(model.paddingStart)
    } else {
        0
    }

    val paddingEnd: Int = if (model.paddingEnd != null) {
        resourceProvider.getDimension(model.paddingEnd)
    } else {
        0
    }
}