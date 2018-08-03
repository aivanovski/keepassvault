package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import javax.inject.Inject

class NotePresenter(private var context: Context,
                    private var noteUid: UUID?,
                    private val view: NoteContract.View): NoteContract.Presenter {

	@Inject
	lateinit var interactor: NoteInteractor

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
		val disposable = interactor.getNoteByUid(noteUid!!)
				.subscribe({ result -> onNoteLoaded(result)})

		disposables.add(disposable)
	}

	private fun onNoteLoaded(result: OperationResult<Note>) {
		if (result.result != null) {
			view.showNote(result.result)
			view.setState(FragmentState.DISPLAYING_DATA)
		} else {
			view.showError(errorInteractor.processAndGetMessage(result.error))
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