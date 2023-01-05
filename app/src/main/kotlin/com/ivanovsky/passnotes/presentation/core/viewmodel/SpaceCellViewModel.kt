package com.ivanovsky.passnotes.presentation.core.viewmodel

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel

class SpaceCellViewModel(
    override val model: SpaceCellModel,
    resourceProvider: ResourceProvider
) : BaseCellViewModel(model) {

    val height: Int = resourceProvider.getDimension(model.height)
}