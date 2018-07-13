package com.ivanovsky.passnotes.ui.notes

import com.ivanovsky.passnotes.ui.core.BasePresenter
import com.ivanovsky.passnotes.ui.core.BaseView

class NotesContract {

	interface View: BaseView<Presenter> {
	}

	interface Presenter: BasePresenter {
	}
}