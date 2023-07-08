package com.ivanovsky.passnotes.presentation.core.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.model.MessageCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class DatabaseStatusCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createDefaultStatusCellModel(): MessageCellModel {
        return MessageCellModel(
            text = EMPTY,
            backgroundColor = resourceProvider.getColor(R.color.light_grey),
            isVisible = false
        )
    }

    fun createStatusCellModel(status: SyncStatus): MessageCellModel {
        return MessageCellModel(
            text = status.name,
            backgroundColor = resourceProvider.getColor(R.color.light_grey),
            isVisible = true
        )
    }
}