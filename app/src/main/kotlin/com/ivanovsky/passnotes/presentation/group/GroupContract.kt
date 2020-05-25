package com.ivanovsky.passnotes.presentation.group

import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView
import com.ivanovsky.passnotes.presentation.core.GlobalSnackbarPresenter

class GroupContract {

    interface View : BaseView<Presenter> {
        fun setTitleEditTextError(error: String?)
        fun setDoneButtonVisibility(isVisible: Boolean)
        fun finishScreen()
    }

    interface Presenter : BasePresenter, GlobalSnackbarPresenter {
        fun loadData()
        fun createNewGroup(title: String)
    }
}