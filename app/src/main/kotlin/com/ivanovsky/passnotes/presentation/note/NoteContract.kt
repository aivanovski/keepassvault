package com.ivanovsky.passnotes.presentation.note

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

class NoteContract {

	//TODO: rewrite with LiveData

	interface View: BaseView<Presenter> {
		fun showNote(note: Note)
	}

	interface Presenter: BasePresenter {
		fun loadData()
		fun onEditNoteButtonClicked()
		fun onCopyToClipboardClicked(text: String)
	}
}
