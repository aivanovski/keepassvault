package com.ivanovsky.passnotes.presentation.storagelist.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SingleTextCellModel
import com.ivanovsky.passnotes.presentation.core.model.TwoTextWithIconCellModel

class StorageListCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createCellModel(options: List<StorageOption>): List<BaseCellModel> {
        return options.map { option ->
            if (option.type == StorageOptionType.SAF_STORAGE) {
                TwoTextWithIconCellModel(
                    id = option.type.name,
                    title = option.title,
                    description = resourceProvider.getString(R.string.saf_warning_message),
                    iconResId = R.drawable.ic_info_grey_600_24dp
                )
            } else {
                SingleTextCellModel(
                    id = option.type.name,
                    text = option.title
                )
            }
        }
    }
}