package com.ivanovsky.passnotes.presentation.storagelist.converter

import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SingleTextCellModel

fun List<StorageOption>.toCellModels(): List<BaseCellModel> {
    return map {
        SingleTextCellModel(
            it.type.name,
            it.title
        )
    }
}