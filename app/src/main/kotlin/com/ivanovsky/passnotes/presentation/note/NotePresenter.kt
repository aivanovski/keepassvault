package com.ivanovsky.passnotes.presentation.note

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
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
) : NoteContract.Presenter,
    ObserverBus.NoteContentChangedObserver {

    @Inject
    lateinit var interactor: NoteInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var observerBus: ObserverBus

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var note: Note? = null

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            view.screenState = ScreenState.loading()

            observerBus.register(this)

            loadData()
        }
    }

    override fun destroy() {
        observerBus.unregister(this)
        job.cancel()
    }

    override fun loadData() {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getNoteByUid(noteUid!!)// TODO: fix !!
            }

            if (result.isSucceededOrDeferred) {
                val n = result.obj
                note = n

                view.showNote(n)
                view.setActionBarTitle(n.title)
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    override fun onEditNoteButtonClicked() {
        val note = this.note ?: return

        view.showEditNoteScreen(note)
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

    override fun onNoteContentChanged(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID) {
        if (oldNoteUid == noteUid) {
            noteUid = newNoteUid

            loadData()
        }
    }
}