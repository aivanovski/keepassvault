package com.ivanovsky.passnotes.presentation.core.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus.NORMAL
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus.POSTPONED_CHANGES
import com.ivanovsky.passnotes.extensions.getNameResId
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

    fun createStatusCellModel(status: DatabaseStatus): MessageCellModel {
        return MessageCellModel(
            text = status.getNameResId()?.let { resourceProvider.getString(it) } ?: EMPTY,
            backgroundColor = resourceProvider.getColor(R.color.light_grey),
            isVisible = (status != NORMAL && status != POSTPONED_CHANGES)
        )
    }
}