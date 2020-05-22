package com.ivanovsky.passnotes.presentation.note

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class NotePresenter(
    private var noteUid: UUID?,
    private val view: NoteContract.View
) : NoteContract.Presenter {

    @Inject
    lateinit var interactor: NoteInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var resourceHelper: ResourceHelper

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().encryptedDatabaseComponent.inject(this)
    }

    override fun start() {
        loadData()
    }

    override fun destroy() {
        job.cancel()
    }

    override fun loadData() {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getNoteByUid(noteUid!!)// TODO: fix !!
            }

            if (result.isSucceededOrDeferred) {
                view.showNote(result.obj)
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    override fun onEditNoteButtonClicked() {
        //TODO: implement
    }

    override fun onCopyToClipboardClicked(text: String) {
        interactor.copyToClipboardWithTimeout(text)

        val delayInSeconds = interactor.getTimeoutValueInMillis() / 1000L

        val message = resourceHelper.getString(
            R.string.copied_clipboard_will_be_cleared_in_seconds,
            delayInSeconds
        )
        view.showSnackbarMessage(message)
    }
}