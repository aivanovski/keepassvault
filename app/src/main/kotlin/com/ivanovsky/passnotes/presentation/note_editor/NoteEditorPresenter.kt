package com.ivanovsky.passnotes.presentation.note_editor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextDataItem
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class NoteEditorPresenter(
    private val view: NoteEditorContract.View,
    private val launchMode: LaunchMode,
    private val groupUid: UUID?,
    private val noteUid: UUID?
) : NoteEditorContract.Presenter {

    @Inject
    lateinit var interactor: NoteEditorInteractor

    @Inject
    lateinit var resourceHelper: ResourceHelper

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().encryptedDatabaseComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            if (launchMode == LaunchMode.NEW) {
                view.setEditorItems(createDefaultEditorItems())
                view.setDoneButtonVisibility(true)
                view.screenState = ScreenState.data()

            } else if (launchMode == LaunchMode.EDIT) {
                view.setDoneButtonVisibility(false)
                view.screenState = ScreenState.loading()

                loadData()
            }
        }
    }

    override fun destroy() {
        job.cancel()
    }

    private fun createDefaultEditorItems(): List<BaseDataItem> {
        return listOf(TextDataItem(ITEM_ID_TITLE, resourceHelper.getString(R.string.name), ""))
    }

    override fun loadData() {
        // TODO: implement
        scope.launch {

        }
    }

    override fun onDoneButtonClicked(items: List<BaseDataItem>) {
        val groupUid = this.groupUid ?: return

        if (launchMode == LaunchMode.NEW) {
            val note = createNoteFromEditorItems(items, groupUid)
            view.setDoneButtonVisibility(false)
            view.screenState = ScreenState.loading()

            scope.launch {
                val createNoteResult = withContext(Dispatchers.Default) {
                    interactor.createNewNote(note)
                }

                if (createNoteResult.isSucceededOrDeferred) {
                    view.finishScreen()
                } else {
                    val message = errorInteractor.processAndGetMessage(createNoteResult.error)
                    view.setDoneButtonVisibility(true)
                    view.screenState = ScreenState.dataWithError(message)
                }
            }
        } else if (launchMode == LaunchMode.EDIT) {

        }
    }

    private fun createNoteFromEditorItems(items: List<BaseDataItem>, groupUid: UUID): Note {
        val note = Note()

        note.groupUid = groupUid
        note.title = getValueByItemId(ITEM_ID_TITLE, items)
        note.created = Date(System.currentTimeMillis())
        note.modified = note.created

        return note
    }

    private fun getValueByItemId(id: Int, items: List<BaseDataItem>): String? {
        return items.first { item -> item.id == id }
            .value
    }

    companion object {
        const val ITEM_ID_TITLE = 1
    }
}