package com.ivanovsky.passnotes.presentation.addeditnote

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

class AddEditNoteContract {

	interface View: BaseView<Presenter> {
		fun showNote(note: Note)
		fun editNote(note: Note)
		fun showError(message: String)
	}

	interface Presenter: BasePresenter {
	}
}
