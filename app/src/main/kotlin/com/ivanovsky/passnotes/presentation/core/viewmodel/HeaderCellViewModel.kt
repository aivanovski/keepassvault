package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel

class HeaderCellViewModel(
    override val model: HeaderCellModel,
    resourceProvider: ResourceProvider
) : BaseCellViewModel(model) {

    val paddingHorizontal: Int = if (model.paddingHorizontal != null) {
        resourceProvider.getDimension(model.paddingHorizontal)
    } else {
        0
    }
}