package com.ivanovsky.passnotes.presentation.note_editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core_mvvm.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.PropertyViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.SecretPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.TextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.util.toCleanString
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class NoteEditorViewModel(
    private val interactor: NoteEditorInteractor,
    private val resources: ResourceProvider,
    private val errorInteractor: ErrorInteractor,
    private val noteDiffer: NoteDiffer,
    private val dispatchers: DispatcherProvider,
    private val modelFactory: NoteEditorCellModelFactory,
    private val viewModelFactory: NoteEditorCellViewModelFactory
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes() // TODO: add view types
        .add(TextPropertyCellViewModel::class, R.layout.cell_text_property)
        .add(SecretPropertyCellViewModel::class, R.layout.cell_secret_property)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.notInitialized())

    val isDoneButtonVisible = MutableLiveData<Boolean>()
    val showDiscardDialogEvent = SingleLiveEvent<String>()
    val finishScreenEvent = SingleLiveEvent<Unit>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()

    private lateinit var launchMode: LaunchMode
    private var groupUid: UUID? = null
    private var noteUid: UUID? = null
    private var note: Note? = null
    private var template: Template? = null

    init {
        subscribeToEvents()
    }

    fun start(args: NoteEditorArgs) {
        launchMode = args.launchMode
        noteUid = args.noteUid
        groupUid = args.groupUid

        if (launchMode == LaunchMode.NEW) {
            val models = modelFactory.createModelsForNewNote(args.template)
            val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
            setCellElements(viewModels)

            isDoneButtonVisible.value = true
            screenState.value = ScreenState.data()

        } else if (launchMode == LaunchMode.EDIT) {
            isDoneButtonVisible.value = false
            screenState.value = ScreenState.loading()

            loadData()
        }
    }

    fun onDoneMenuClicked() {
        if (!isAllDataValid()) {
            displayError()
            return
        }

        if (launchMode == LaunchMode.NEW) {
            val groupUid = this.groupUid ?: return

            val note = createNoteFromCells(groupUid, template)
            isDoneButtonVisible.value = false
            hideKeyboardEvent.call()
            screenState.value = ScreenState.loading()

            viewModelScope.launch {
                val createNoteResult = withContext(Dispatchers.Default) {
                    interactor.createNewNote(note)
                }

                if (createNoteResult.isSucceededOrDeferred) {
                    finishScreenEvent.call()
                } else {
                    val message = errorInteractor.processAndGetMessage(createNoteResult.error)
                    isDoneButtonVisible.value = true
                    screenState.value = ScreenState.dataWithError(message)
                }
            }
        } else if (launchMode == LaunchMode.EDIT) {
//            val existingNote = loadedNote ?: return
//            val existingTemplate = loadedTemplate
//
//            view.setDoneButtonVisibility(false)
//            view.hideKeyboard()
//            view.screenState = com.ivanovsky.passnotes.presentation.core.ScreenState.loading()
//
//            val modifiedNote = createModifiedNoteFromEditorItems(filteredItems, existingNote, existingTemplate)
//            if (isNoteChanged(existingNote, modifiedNote)) {
//                scope.launch {
//                    val updateNoteResult = withContext(Dispatchers.Default) {
//                        interactor.updateNote(modifiedNote)
//                    }
//
//                    if (updateNoteResult.isSucceededOrDeferred) {
//                        view.finishScreen()
//                    } else {
//                        val message = errorInteractor.processAndGetMessage(updateNoteResult.error)
//                        view.setDoneButtonVisibility(true)
//                        view.screenState = com.ivanovsky.passnotes.presentation.core.ScreenState.dataWithError(message)
//                    }
//                }
//            } else {
//                view.showToastMessage(resources.getString(R.string.no_changes))
//                view.finishScreen()
//            }
        }
    }

    fun onDiscardConfirmed() {
        finishScreenEvent.call()
    }

    fun onBackClicked() {
        val properties = createPropertiesFromCells()

        when (launchMode) {
            LaunchMode.NEW -> {
                if (properties.isEmpty()) {
                    finishScreenEvent.call()
                } else {
                    showDiscardDialogEvent.call(resources.getString(R.string.discard_changes))
                }
            }
            LaunchMode.EDIT -> {
                // TODO: implement
                val existingNote = note ?: return

//                val modifiedNote = createModifiedNoteFromEditorItems(items, existingNote, template)
//                if (isNoteChanged(existingNote, modifiedNote)) {
//                    showDiscardDialogEvent.call(resources.getString(R.string.discard_changes))
//                } else {
//                    finishScreenEvent.call()
//                }
            }
        }
    }

    private fun loadData() {
        val uid = noteUid ?: return

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val noteResult = withContext(dispatchers.IO) {
                interactor.loadNote(uid)
            }

            if (noteResult.isSucceededOrDeferred) {
                note = noteResult.obj

                val note = noteResult.obj

                template = withContext(dispatchers.IO) {
                    loadTemplate(note)
                }
//
//                editorDataTransformer = NoteEditorDataTransformer(loadedTemplate)
//                loadedNote = note
//
//                view.setEditorItems(editorDataTransformer.createNoteToEditorItems(note))
                isDoneButtonVisible.value = true
                screenState.value = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(noteResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun loadTemplate(note: Note): Template? {
        val filter = PropertyFilter.Builder()
            .filterTemplateUid()
            .build()

        val templateUid = filter.apply(note.properties)
            .firstOrNull()
            ?.value
            ?.toUUID()

        return if (templateUid != null) {
            interactor.loadTemplate(templateUid)
        } else {
            null
        }
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->

        }
    }

    private fun createPropertiesFromCells(): List<Property> {
        val properties = getViewModels()
            .map { it.createProperty() }

        val filter = PropertyFilter.Builder()
            .notEmpty()
            .build()

        return filter.apply(properties)
    }

    private fun isAllDataValid(): Boolean {
        return getViewModels()
            .all { it.isDataValid() }
    }

    private fun displayError() {
        getViewModels()
            .onEach {
                if (!it.isDataValid()) {
                    it.displayError()
                }
            }
    }

    private fun getViewModels(): List<PropertyViewModel> {
        return cellViewModels.value
            ?.mapNotNull { it as? PropertyViewModel }
            ?: emptyList()
    }

    private fun createNoteFromCells(
        groupUid: UUID,
        template: Template?
    ): Note {
        val properties = createPropertiesFromCells().toMutableList()

        val title = PropertyFilter.Builder()
            .includeTitle()
            .build()
            .apply(properties)
            .firstOrNull()
            ?.value
            ?: ""

        val created = Date(System.currentTimeMillis())

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

    object CellId {
        const val TITLE = "title"
        const val USER_NAME = "userName"
        const val URL = "url"
        const val EMAIL = "email"
        const val NOTES = "notes"
        const val PASSWORD = "password"
    }
}