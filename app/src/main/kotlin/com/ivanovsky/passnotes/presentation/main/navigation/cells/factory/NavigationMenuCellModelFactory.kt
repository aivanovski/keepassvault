package com.ivanovsky.passnotes.presentation.main.navigation.cells.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SingleTextWithIconCellModel
import com.ivanovsky.passnotes.presentation.main.navigation.cells.model.NavigationHeaderCellModel

class NavigationMenuCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun creteHeaderModel(): List<BaseCellModel> {
        return listOf(
            NavigationHeaderCellModel(
                id = CellId.HEADER,
                text = resourceProvider.getString(R.string.app_name)
            )
        )
    }

    fun createMenuItemModels(
        isDatabaseOpened: Boolean,
        isDebugMenuVisible: Boolean
    ): List<BaseCellModel> {
        return mutableListOf<BaseCellModel>()
            .apply {
                if (isDatabaseOpened) {
                    add(newModelById(CellId.LOCK))
                } else {
                    add(newModelById(CellId.SELECT_FILE))
                }

                add(newModelById(CellId.SETTINGS))

                if (isDebugMenuVisible) {
                    add(newModelById(CellId.DEBUG_MENU))
                }

                add(newModelById(CellId.ABOUT))
            }
    }

    private fun newModelById(cellId: Int): BaseCellModel {
        return when (cellId) {
            CellId.SELECT_FILE -> SingleTextWithIconCellModel(
                id = cellId,
                title = resourceProvider.getString(R.string.select_file),
                iconResId = R.drawable.ic_folder_grey_600_24dp
            )
            CellId.LOCK -> SingleTextWithIconCellModel(
                id = cellId,
                title = resourceProvider.getString(R.string.lock),
                iconResId = R.drawable.ic_lock_grey_600_24dp
            )
            CellId.SETTINGS -> SingleTextWithIconCellModel(
                id = cellId,
                title = resourceProvider.getString(R.string.settings),
                iconResId = R.drawable.ic_settings_grey_600_24dp
            )
            CellId.DEBUG_MENU -> SingleTextWithIconCellModel(
                id = cellId,
                title = resourceProvider.getString(R.string.debug_menu),
                iconResId = R.drawable.ic_developer_mode_grey_600_24dp
            )
            CellId.ABOUT -> SingleTextWithIconCellModel(
                id = cellId,
                title = resourceProvider.getString(R.string.about),
                iconResId = R.drawable.ic_info_grey_600_24dp
            )
            else -> throw IllegalArgumentException()
        }
    }

    object CellId {
        const val HEADER = 1
        const val SELECT_FILE = 10
        const val LOCK = 20
        const val SETTINGS = 30
        const val DEBUG_MENU = 40
        const val ABOUT = 50
    }
}