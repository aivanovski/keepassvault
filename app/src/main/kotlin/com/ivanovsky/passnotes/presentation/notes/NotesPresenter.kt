package com.ivanovsky.passnotes.presentation.notes

import android.os.Handler
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor
import com.ivanovsky.passnotes.injection.Injector
import java9.util.concurrent.CompletableFuture
import java9.util.function.Supplier
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject

class NotesPresenter(private val groupUid: UUID,
                     private val view: NotesContract.View) : NotesContract.Presenter {

	@Inject
	lateinit var interactor: NotesInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var executor: Executor

	private val handler = Handler()

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun start() {
		loadData()
	}

	override fun stop() {
	}

	override fun loadData() {
        CompletableFuture.supplyAsync(Supplier {
	        interactor.getNotesByGroupUid(groupUid)
        }, executor)
		        .thenAccept { result -> onGetNotesResult(result) }
	}

	private fun onGetNotesResult(result: OperationResult<List<Note>>) {
		handler.post {
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