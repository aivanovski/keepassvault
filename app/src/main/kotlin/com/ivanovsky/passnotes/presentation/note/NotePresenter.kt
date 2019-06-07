package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import android.os.Handler
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import java9.util.concurrent.CompletableFuture
import java9.util.function.Supplier
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject

class NotePresenter(private var context: Context,
                    private var noteUid: UUID?,
                    private val view: NoteContract.View): NoteContract.Presenter {

	@Inject
	lateinit var interactor: NoteInteractor

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
			interactor.getNoteByUid(noteUid!!)
		}, executor)
				.thenAccept { result -> onGetNoteResult(result) }
	}

	private fun onGetNoteResult(result: OperationResult<Note>) {
		handler.post {
			if (result.isSucceededOrDeferred) {
				view.showNote(result.obj)
				view.setState(FragmentState.DISPLAYING_DATA)
			} else {
				view.showError(errorInteractor.processAndGetMessage(result.error))
			}
		}
	}

	override fun onEditNoteButtonClicked() {
		//TODO: implement
	}

	override fun onCopyToClipboardClicked(text: String) {
		interactor.copyToClipboardWithTimeout(text)

		val delayInSeconds = interactor.getTimeoutValueInMillis() / 1000L

		view.showSnackbar(context.getString(R.string.copied_clipboard_will_be_cleared_in_seconds, delayInSeconds))
	}
}