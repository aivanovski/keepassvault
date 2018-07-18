package com.ivanovsky.passnotes.ui.notes

import android.content.Context
import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider
import com.ivanovsky.passnotes.data.safedb.model.Note
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

class NotesPresenter(private val groupUid: UUID, private val context: Context, private val view: NotesContract.View):
		NotesContract.Presenter {

	@Inject
	lateinit var dbProvider: EncryptedDatabaseProvider

	private var disposables = CompositeDisposable()

	init {
		App.getDaggerComponent().inject(this)
	}

	override fun start() {
		loadData()
	}

	override fun stop() {
		disposables.clear()
	}

	override fun loadData() {
		if (dbProvider.isOpened) {
			val noteRepository = dbProvider.database.noteRepository

			val disposable = noteRepository.getNotesByGroupUid(groupUid)
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe({ notes -> onNotesLoaded(notes) })

			disposables.add(disposable)
		} else {
			view.showUnlockScreenAndFinish()
		}
	}

	private fun onNotesLoaded(notes: List<Note>) {
		if (notes.isNotEmpty()) {
			view.showNotes(notes)
		} else {
			view.showNotItems()
		}
	}
}