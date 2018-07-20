package com.ivanovsky.passnotes.ui.notes

import android.content.Context
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor
import com.ivanovsky.passnotes.injection.Injector
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import javax.inject.Inject

class NotesPresenter(private val groupUid: UUID, private val context: Context, private val view: NotesContract.View):
		NotesContract.Presenter {

	@Inject
	lateinit var interactor: NotesInteractor

	private var disposables = CompositeDisposable()

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun start() {
		loadData()
	}

	override fun stop() {
		disposables.clear()
	}

	override fun loadData() {
		val disposable = interactor.getNotesByGroupUid(groupUid)
				.subscribe({ notes -> onNotesLoaded(notes)})

		disposables.add(disposable)
	}

	private fun onNotesLoaded(notes: List<Note>) {
		if (notes.isNotEmpty()) {
			view.showNotes(notes)
		} else {
			view.showNotItems()
		}
	}
}