package com.ivanovsky.passnotes.presentation.notes

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor
import com.ivanovsky.passnotes.injection.Injector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class NotesPresenter(private val groupUid: UUID,
                     private val view: NotesContract.View) : NotesContract.Presenter {

	@Inject
	lateinit var interactor: NotesInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	private val scope = CoroutineScope(Dispatchers.IO)

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun start() {
		loadData()
	}

	override fun stop() {
	}

	override fun loadData() {
		scope.launch(Dispatchers.IO) {
			val notes = interactor.getNotesByGroupUid(groupUid)

			withContext(Dispatchers.Main) {
				onNotesLoadedResult(notes)
			}
		}
	}

	private fun onNotesLoadedResult(result: OperationResult<List<Note>>) {
		if (result.isSucceededOrDeferred) {
			val notes = result.obj

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