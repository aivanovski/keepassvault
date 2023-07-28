package com.ivanovsky.passnotes.presentation.storagelist.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SingleTextCellModel
import com.ivanovsky.passnotes.presentation.core.model.TwoTextWithIconCellModel

class StorageListCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createCellModel(options: List<StorageOption>): List<BaseCellModel> {
        val fsTypes = options.map { option -> option.root.fsAuthority.type }
            .toSet()

        val isExternalStorageEnabled = fsTypes.contains(FSType.EXTERNAL_STORAGE)

        val result = mutableListOf<BaseCellModel>()
        for (option in options) {
            val model = when (val fsType = option.root.fsAuthority.type) {
                FSType.EXTERNAL_STORAGE -> {
                    TwoTextWithIconCellModel(
                        id = fsType.value,
                        title = fsType.getTitle(isExternalStorageEnabled),
                        description = resourceProvider.getString(R.string.requires_permission),
                        iconResId = R.drawable.ic_info_24dp
                    )
                }

                else -> {
                    SingleTextCellModel(
                        id = fsType.value,
                        text = fsType.getTitle(isExternalStorageEnabled)
                    )
                }
            }

            result.add(model)
        }

        return result
    }

    private fun FSType.getTitle(isExternalStorageEnabled: Boolean): String {
        val titleId = when (this) {
            FSType.INTERNAL_STORAGE -> R.string.private_app_storage
            FSType.EXTERNAL_STORAGE -> R.string.external_storage_app_picker
            FSType.WEBDAV -> R.string.webdav
            FSType.GIT -> R.string.git
            FSType.FAKE -> R.string.fake_file_system
            FSType.SAF -> {
                if (isExternalStorageEnabled) {
                    R.string.external_storage_system_picker
                } else {
                    R.string.external_storage
                }
            }

            FSType.UNDEFINED -> throw IllegalArgumentException()
        }

        return resourceProvider.getString(titleId)
    }
}