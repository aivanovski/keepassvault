package com.ivanovsky.passnotes.ui.notes

import android.content.Context
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor
import com.ivanovsky.passnotes.injection.Injector
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import javax.inject.Inject

class NotesPresenter(private val groupUid: UUID, private val context: Context, private val view: NotesContract.View):
		NotesContract.Presenter {

	@Inject
	lateinit var interactor: NotesInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

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
				.subscribe({ result -> onNotesLoadedResult(result)})

		disposables.add(disposable)
	}

	private fun onNotesLoadedResult(result: OperationResult<List<Note>>) {
		if (result.result != null) {
			val notes = result.result

			if (notes.isNotEmpty()) {
				view.showNotes(notes)
			} else {
				view.showNotItems()
			}
		} else {
			view.showError(errorInteractor.processAndGetMessage(result.error))
		}
	}
}