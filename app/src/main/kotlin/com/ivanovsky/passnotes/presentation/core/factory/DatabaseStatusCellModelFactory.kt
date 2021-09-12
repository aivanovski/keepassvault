package com.ivanovsky.passnotes.presentation.core.factory

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.extensions.getNameResId
import com.ivanovsky.passnotes.presentation.core.model.DatabaseStatusCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class DatabaseStatusCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createDefaultStatusCellModel(): DatabaseStatusCellModel {
        return DatabaseStatusCellModel(
            text = EMPTY,
            isVisible = false
        )
    }

    fun createStatusCellModel(status: DatabaseStatus): DatabaseStatusCellModel {
        return DatabaseStatusCellModel(
            text = status.getNameResId()?.let { resourceProvider.getString(it) } ?: EMPTY,
            isVisible = (status != DatabaseStatus.NORMAL)
        )
    }
}