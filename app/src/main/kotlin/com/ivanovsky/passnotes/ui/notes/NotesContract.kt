package com.ivanovsky.passnotes.ui.notes

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

class NotesContract {

	interface View: BaseView<Presenter> {
		fun showNotes(notes: List<Note>)
		fun showNotItems()
		fun showUnlockScreenAndFinish()
	}

	interface Presenter: BasePresenter {
		fun loadData()
	}
}