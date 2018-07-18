package com.ivanovsky.passnotes.ui.notes

import com.ivanovsky.passnotes.data.safedb.model.Note
import com.ivanovsky.passnotes.ui.core.BasePresenter
import com.ivanovsky.passnotes.ui.core.BaseView

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