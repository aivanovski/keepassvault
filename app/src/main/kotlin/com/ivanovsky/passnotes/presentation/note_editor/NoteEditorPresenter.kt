package com.ivanovsky.passnotes.presentation.note_editor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.injection.DaggerInjector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode.EDIT
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorContract.LaunchMode.NEW
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.NoteEditorDataTransformer
import com.ivanovsky.passnotes.presentation.note_editor.view.extended_text.ExtTextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType
import com.ivanovsky.passnotes.util.toCleanString
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class NoteEditorPresenter(
    private val view: NoteEditorContract.View,
    private val launchMode: LaunchMode,
    private val groupUid: UUID?,
    private val noteUid: UUID?,
    private val template: Template?
) : NoteEditorContract.Presenter {

    @Inject
    lateinit var interactor: NoteEditorInteractor

    @Inject
    lateinit var resources: ResourceProvider

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var noteDiffer: NoteDiffer

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var editorDataTransformer: NoteEditorDataTransformer

    private var loadedNote: Note? = null
    private var loadedTemplate: Template? = null

    init {
        DaggerInjector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            if (launchMode == NEW) {
                editorDataTransformer = NoteEditorDataTransformer(template)

                view.setEditorItems(editorDataTransformer.createEditorItemsForNewNote())
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

    override fun loadData() {
        val uid = noteUid ?: return

        view.screenState = ScreenState.loading()

        scope.launch {
            val noteResult = withContext(Dispatchers.Default) {
                interactor.loadNote(uid)
            }

            if (noteResult.isSucceededOrDeferred) {
                val note = noteResult.obj

                loadedTemplate = withContext(Dispatchers.Default) {
                    loadTemplate(note)
                }

                editorDataTransformer = NoteEditorDataTransformer(loadedTemplate)
                loadedNote = note

                view.setEditorItems(editorDataTransformer.createNoteToEditorItems(note))
                view.setDoneButtonVisibility(true)
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(noteResult.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    private fun loadTemplate(note: Note): Template? {
        val propertySpreader = PropertySpreader(note.properties)
        val templateUid = propertySpreader.findTemplateUid()?.toUUID()

        return if (templateUid != null) {
            interactor.loadTemplate(templateUid)
        } else {
            null
        }
    }

    override fun onDoneButtonClicked(items: List<BaseDataItem>) {
        val filteredItems = editorDataTransformer.filterNotEmptyItems(items)

        if (launchMode == NEW) {
            val groupUid = this.groupUid ?: return

            val note = createNewNoteFromEditorItems(filteredItems, groupUid, template)
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
            val existingNote = loadedNote ?: return
            val existingTemplate = loadedTemplate

            view.setDoneButtonVisibility(false)
            view.hideKeyboard()
            view.screenState = ScreenState.loading()

            val modifiedNote = createModifiedNoteFromEditorItems(filteredItems, existingNote, existingTemplate)
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

    private fun createNewNoteFromEditorItems(
        items: List<BaseDataItem>,
        groupUid: UUID,
        template: Template?
    ): Note {
        val title = editorDataTransformer.getTitleFromItems(items) ?: ""
        val created = Date(System.currentTimeMillis())
        val properties = editorDataTransformer.createPropertiesFromItems(items).toMutableList()

        if (template != null) {
            properties.add(
                Property(
                    null,
                    Property.PROPERTY_NAME_TEMPLATE_UID,
                    template.uid.toCleanString()
                )
            )
        }

        return Note(null, groupUid, created, created, title, properties)
    }

    private fun createModifiedNoteFromEditorItems(
        items: List<BaseDataItem>,
        existingNote: Note,
        existingTemplate: Template?
    ): Note {
        val title = editorDataTransformer.getTitleFromItems(items) ?: ""
        val modified = Date(System.currentTimeMillis())

        val propertySpreader = PropertySpreader(existingNote.properties)

        val hiddenProperties = propertySpreader.getHiddenProperties()
        val modifiedProperties = editorDataTransformer.createPropertiesFromItems(items)
        val properties = (hiddenProperties + modifiedProperties).toMutableList()

        if (existingTemplate != null && !propertySpreader.hasTemplateUidProperty()) {
            properties.add(
                Property(
                    null,
                    Property.PROPERTY_NAME_TEMPLATE_UID,
                    existingTemplate.uid.toCleanString()
                )
            )
        }

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
                val existingNote = loadedNote ?: return

                val modifiedNote = createModifiedNoteFromEditorItems(items, existingNote, template)
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