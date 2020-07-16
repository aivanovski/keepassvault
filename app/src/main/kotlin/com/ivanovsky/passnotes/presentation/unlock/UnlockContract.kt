package com.ivanovsky.passnotes.presentation.unlock

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

class UnlockContract {

    interface View : BaseView<Presenter> {
        fun setRecentlyUsedItems(items: List<UnlockFragment.DropDownItem>)
        fun setSelectedRecentlyUsedItem(position: Int)
        fun showGroupsScreen()
        fun showNewDatabaseScreen()
        fun showOpenFileScreen()
        fun showSettingScreen()
        fun showAboutScreen()
        fun showDebugMenuScreen()
    }

    interface Presenter : BasePresenter {

        fun loadData(resetSelection: Boolean)
        fun onRecentlyUsedItemSelected(position: Int)
        fun onUnlockButtonClicked(password: String)
        fun onOpenFileMenuClicked()
        fun onSettingsMenuClicked()
        fun onAboutMenuClicked()
        fun onDebugMenuClicked()
        fun onFilePicked(file: FileDescriptor)
        fun closeActiveDatabaseIfNeed()
    }
}