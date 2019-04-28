package com.ivanovsky.passnotes.presentation.note

import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.util.COROUTINE_EXCEPTION_HANDLER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class NotePresenter(private var context: Context,
                    private var noteUid: UUID?,
                    private val view: NoteContract.View): NoteContract.Presenter {

	@Inject
	lateinit var interactor: NoteInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	private val scope = CoroutineScope(Dispatchers.Main + COROUTINE_EXCEPTION_HANDLER)

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun start() {
		loadData()
	}

	override fun stop() {
	}

	override fun loadData() {
		scope.launch {
			val result = withContext(Dispatchers.Default) {
				interactor.getNoteByUid(noteUid!!)// TODO: fix !!
			}

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