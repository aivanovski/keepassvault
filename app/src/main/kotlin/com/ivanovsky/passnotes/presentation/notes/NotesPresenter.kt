package com.ivanovsky.passnotes.presentation.notes

import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor
import com.ivanovsky.passnotes.injection.Injector
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class NotesPresenter(private val groupUid: UUID,
                     private val view: NotesContract.View) : NotesContract.Presenter {

	@Inject
	lateinit var interactor: NotesInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

    private val job = Job()
	private val scope = CoroutineScope(Dispatchers.Main + job)

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun start() {
		loadData()
	}

	override fun stop() {
	}

	override fun destroy() {
		job.cancel()
	}

	override fun loadData() {
		scope.launch {
			val result = withContext(Dispatchers.Main) {
				interactor.getNotesByGroupUid(groupUid)
			}

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
}