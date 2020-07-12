package com.ivanovsky.passnotes.presentation.note_editor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode.EDIT
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode.NEW
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_NOTES
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_PASSWORD
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_TITLE
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_URL
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_USER_NAME
import com.ivanovsky.passnotes.presentation.note_editor.view.NoteEditorDataTransformer
import com.ivanovsky.passnotes.presentation.note_editor.view.extended_text.ExtTextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.text.InputLines
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType
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
    lateinit var resources: ResourceHelper

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var noteDiffer: NoteDiffer

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val editorDataTransformer =
        NoteEditorDataTransformer()

    private var note: Note? = null

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            if (launchMode == NEW) {
                view.setEditorItems(createDefaultEditorItems())
                view.setDoneButtonVisibility(true)
                view.screenState = ScreenState.data()

            } else if (launchMode == EDIT) {
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
        return listOf(
            TextDataItem(
                ITEM_ID_TITLE,
                resources.getString(R.string.title),
                "",
                TextInputType.TEXT,
                InputLines.SINGLE_LINE,
                isShouldNotBeEmpty = true
            ),
            TextDataItem(
                ITEM_ID_USER_NAME,
                resources.getString(R.string.username),
                "",
                TextInputType.TEXT,
                InputLines.SINGLE_LINE
            ),
            SecretDataItem(
                ITEM_ID_PASSWORD,
                resources.getString(R.string.password),
                "",
                SecretInputType.TEXT
            ),
            TextDataItem(
                ITEM_ID_URL,
                resources.getString(R.string.url_cap),
                "",
                TextInputType.URL,
                InputLines.SINGLE_LINE
            ),
            TextDataItem(
                ITEM_ID_NOTES,
                resources.getString(R.string.notes),
                "",
                TextInputType.TEXT_CAP_SENTENCES,
                InputLines.MULTIPLE_LINES
            )
        )
    }

    override fun loadData() {
        val uid = noteUid ?: return

        view.screenState = ScreenState.loading()

        scope.launch {
            val noteResult = withContext(Dispatchers.Default) {
                interactor.loadNote(uid)
            }

            if (noteResult.isSucceededOrDeferred) {
                note = noteResult.obj

                view.setEditorItems(editorDataTransformer.createNoteToEditorItems(noteResult.obj))
                view.setDoneButtonVisibility(true)
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(noteResult.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    override fun onDoneButtonClicked(items: List<BaseDataItem>) {
        val filteredItems = editorDataTransformer.filterNotEmptyItems(items)

        if (launchMode == NEW) {
            val groupUid = this.groupUid ?: return

            val note = createNewNoteFromEditorItems(filteredItems, groupUid)
            view.setDoneButtonVisibility(false)
            view.hideKeyboard()
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
        } else if (launchMode == EDIT) {
            val existingNote = note ?: return

            view.setDoneButtonVisibility(false)
            view.hideKeyboard()
            view.screenState = ScreenState.loading()

            val modifiedNote = createModifiedNoteFromEditorItems(filteredItems, existingNote)
            if (isNoteChanged(existingNote, modifiedNote)) {
                scope.launch {
                    val updateNoteResult = withContext(Dispatchers.Default) {
                        interactor.updateNote(modifiedNote)
                    }

                    if (updateNoteResult.isSucceededOrDeferred) {
                        view.finishScreen()
                    } else {
                        val message = errorInteractor.processAndGetMessage(updateNoteResult.error)
                        view.setDoneButtonVisibility(true)
                        view.screenState = ScreenState.dataWithError(message)
                    }
                }
            } else {
                view.showToastMessage(resources.getString(R.string.no_changes))
                view.finishScreen()
            }
        }
    }

    private fun createNewNoteFromEditorItems(items: List<BaseDataItem>, groupUid: UUID): Note {
        val title = editorDataTransformer.getTitleFromItems(items) ?: ""
        val created = Date(System.currentTimeMillis())
        val properties = editorDataTransformer.createPropertiesFromItems(items)
        return Note(null, groupUid, created, created, title, properties)
    }

    private fun createModifiedNoteFromEditorItems(
        items: List<BaseDataItem>,
        existingNote: Note
    ): Note {
        val title = editorDataTransformer.getTitleFromItems(items) ?: ""
        val modified = Date(System.currentTimeMillis())

        val hiddenProperties = PropertySpreader(existingNote.properties).hiddenProperties
        val modifiedProperties = editorDataTransformer.createPropertiesFromItems(items)
        val properties = hiddenProperties + modifiedProperties

        return Note(
            existingNote.uid,
            existingNote.groupUid,
            existingNote.created,
            modified,
            title,
            properties
        )
    }

    private fun isNoteChanged(existingNote: Note, modifiedNote: Note): Boolean {
        return !noteDiffer.isEqualsByFields(
            existingNote,
            modifiedNote,
            NoteDiffer.ALL_FIELDS_WITHOUT_MODIFIED
        )
    }

    override fun onAddButtonClicked() {
        view.addEditorItem(
            ExtTextDataItem(
                BaseDataItem.ITEM_ID_CUSTOM,
                "",
                "",
                isProtected = false,
                isCollapsed = false,
                textInputType = TextInputType.TEXT
            )
        )
    }

    override fun onBackPressed() {
        val items = editorDataTransformer.filterNotEmptyItems(view.getEditorItems())

        when (launchMode) {
            NEW -> {
                if (items.isEmpty()) {
                    view.finishScreen()
                } else {
                    view.showDiscardDialog(resources.getString(R.string.discard_changes))
                }
            }
            EDIT -> {
                val existingNote = note ?: return

                val modifiedNote = createModifiedNoteFromEditorItems(items, existingNote)
                if (isNoteChanged(existingNote, modifiedNote)) {
                    view.showDiscardDialog(resources.getString(R.string.discard_changes))
                } else {
                    view.finishScreen()
                }
            }
        }
    }

    override fun onDiscardConfirmed() {
        view.finishScreen()
    }
}