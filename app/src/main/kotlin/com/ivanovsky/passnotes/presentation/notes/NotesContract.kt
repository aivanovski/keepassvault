package com.ivanovsky.passnotes.presentation.notes

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.presentation.core.BasePresenter
import com.ivanovsky.passnotes.presentation.core.BaseView

class NotesContract {

    interface View : BaseView<Presenter> {
        fun showNotes(notes: List<Note>)
        fun showUnlockScreenAndFinish()
        fun showNoteScreen(note: Note)
    }

    interface Presenter : BasePresenter {
        fun loadData()
    }
}