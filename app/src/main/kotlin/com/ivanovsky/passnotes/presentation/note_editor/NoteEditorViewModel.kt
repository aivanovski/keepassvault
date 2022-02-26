package com.ivanovsky.passnotes.presentation.note_editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.entity.PropertyMap
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.ExtendedTextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.PropertyViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.SecretPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.TextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toCleanString
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class NoteEditorViewModel(
    private val interactor: NoteEditorInteractor,
    private val resources: ResourceProvider,
    private val errorInteractor: ErrorInteractor,
    private val noteDiffer: NoteDiffer,
    private val dispatchers: DispatcherProvider,
    private val modelFactory: NoteEditorCellModelFactory,
    private val viewModelFactory: NoteEditorCellViewModelFactory,
    private val router: Router
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes()
        .add(TextPropertyCellViewModel::class, R.layout.cell_text_property)
        .add(SecretPropertyCellViewModel::class, R.layout.cell_secret_property)
        .add(ExtendedTextPropertyCellViewModel::class, R.layout.cell_extended_text_property)
        .add(SpaceCellViewModel::class, R.layout.cell_space_two_line)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val isDoneButtonVisible = MutableLiveData<Boolean>()
    val showDiscardDialogEvent = SingleLiveEvent<String>()
    val showAddPropertyDialogEvent = SingleLiveEvent<List<Pair<AddPropertyDialogItem, String>>>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val showToastEvent = SingleLiveEvent<String>()

    private lateinit var mode: NoteEditorMode
    private var groupUid: UUID? = null
    private var noteUid: UUID? = null
    private var note: Note? = null
    private var template: Template? = null

    init {
        subscribeToEvents()
    }

    fun start(args: NoteEditorArgs) {
        mode = args.mode
        noteUid = args.noteUid
        groupUid = args.groupUid
        template = args.template

        if (mode == NoteEditorMode.NEW) {
            val models = when {
                args.template != null -> modelFactory.createModelsFromTemplate(args.template)
                args.properties != null -> modelFactory.createModelsFromProperties(args.properties)
                else -> modelFactory.createDefaultModels()
            }
            val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
            setCellElements(viewModels)

            isDoneButtonVisible.value = true
            screenState.value = ScreenState.data()

        } else if (mode == NoteEditorMode.EDIT) {
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

        if (mode == NoteEditorMode.NEW) {
            val groupUid = this.groupUid ?: return

            val note = createNewNoteFromCells(groupUid, template)
            isDoneButtonVisible.value = false
            hideKeyboardEvent.call()
            screenState.value = ScreenState.loading()

            viewModelScope.launch {
                val createNoteResult = withContext(Dispatchers.Default) {
                    interactor.createNewNote(note)
                }

                if (createNoteResult.isSucceededOrDeferred) {
                    finishScreen()
                } else {
                    val message = errorInteractor.processAndGetMessage(createNoteResult.error)
                    isDoneButtonVisible.value = true
                    screenState.value = ScreenState.dataWithError(message)
                }
            }
        } else if (mode == NoteEditorMode.EDIT) {
            val sourceNote = note ?: return
            val sourceTemplate = template

            isDoneButtonVisible.value = false
            hideKeyboardEvent.call()
            screenState.value = ScreenState.loading()

            val newNote = createModifiedNoteFromCells(sourceNote, sourceTemplate)
            if (isNoteChanged(sourceNote, newNote)) {
                viewModelScope.launch {
                    val updateNoteResult = withContext(Dispatchers.Default) {
                        interactor.updateNote(newNote)
                    }

                    if (updateNoteResult.isSucceededOrDeferred) {
                        finishScreen()
                    } else {
                        val message = errorInteractor.processAndGetMessage(updateNoteResult.error)
                        isDoneButtonVisible.value = true
                        screenState.value = ScreenState.dataWithError(message)
                    }
                }
            } else {
                showToastEvent.call(resources.getString(R.string.no_changes))
                finishScreen()
            }
        }
    }

    fun onDiscardConfirmed() {
        finishScreen()
    }

    fun onBackClicked() {
        val properties = createPropertiesFromCells()

        when (mode) {
            NoteEditorMode.NEW -> {
                if (properties.isEmpty() || isAllDefaultAndEmpty(properties)) {
                    finishScreen()
                } else {
                    showDiscardDialogEvent.call(resources.getString(R.string.discard_changes))
                }
            }
            NoteEditorMode.EDIT -> {
                val sourceNote = note ?: return
                val sourceTemplate = template

                val newNote = createModifiedNoteFromCells(sourceNote, sourceTemplate)
                if (isNoteChanged(sourceNote, newNote)) {
                    showDiscardDialogEvent.call(resources.getString(R.string.discard_changes))
                } else {
                    finishScreen()
                }
            }
        }
    }

    fun onFabButtonClicked() {
        val typesToAdd = determineCellTypesToAdd()

        if (typesToAdd.isNotEmpty()) {
            val dialogItems = typesToAdd
                .map { propertyType ->
                    when (propertyType) {
                        PropertyType.TITLE -> {
                            Pair(
                                AddPropertyDialogItem.TITLE,
                                resources.getString(R.string.title)
                            )
                        }
                        PropertyType.PASSWORD -> {
                            Pair(
                                AddPropertyDialogItem.PASSWORD,
                                resources.getString(R.string.password)
                            )
                        }
                        PropertyType.USER_NAME -> {
                            Pair(
                                AddPropertyDialogItem.USER_NAME,
                                resources.getString(R.string.username)
                            )
                        }
                        PropertyType.URL -> {
                            Pair(
                                AddPropertyDialogItem.URL,
                                resources.getString(R.string.url_cap)
                            )
                        }
                        PropertyType.NOTES -> {
                            Pair(
                                AddPropertyDialogItem.NOTES,
                                resources.getString(R.string.notes)
                            )
                        }
                    }
                }

            showAddPropertyDialogEvent.call(
                dialogItems +
                    Pair(
                        AddPropertyDialogItem.CUSTOM,
                        resources.getString(R.string.custom)
                    )
            )
        } else {
            onAddPropertyClicked(AddPropertyDialogItem.CUSTOM)
        }
    }

    fun onAddPropertyClicked(item: AddPropertyDialogItem) {
        val models = modelFactory.createCustomPropertyModels(item.propertyType)

        val viewModels = getViewModelsWithoutSpace().toMutableList()
        viewModels.addAll(viewModelFactory.createCellViewModels(models, eventProvider))

        setCellElements(viewModels)
    }

    private fun finishScreen() {
        router.backTo(
            GroupsScreen(
                GroupsScreenArgs(
                    appMode = ApplicationLaunchMode.NORMAL,
                    groupUid = groupUid,
                    isCloseDatabaseOnExit = (groupUid == null)
                )
            )
        )
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

                val models = modelFactory.createModelsForNote(note, template)
                val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
                setCellElements(viewModels)

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
            if (event.containsKey(ExtendedTextPropertyCellViewModel.REMOVE_EVENT)) {
                val cellId = event.getString(ExtendedTextPropertyCellViewModel.REMOVE_EVENT)
                if (cellId != null) {
                    val viewModel = findViewModelByCellId(cellId)
                    val viewModels = cellViewModels.value
                        ?.toMutableList()
                        ?: mutableListOf()

                    if (viewModel != null) {
                        viewModels.remove(viewModel)
                    }

                    setCellElements(viewModels)
                }
            }
        }
    }

    private fun createPropertiesFromCells(): List<Property> {
        val rawProperties = getPropertyViewModels()
            .map { it.createProperty() }

        val defaultProperties = PropertyFilter.Builder()
            .filterDefaultTypes()
            .build()
            .apply(rawProperties)

        val customProperties = PropertyFilter.Builder()
            .excludeDefaultTypes()
            .build()
            .apply(rawProperties)

        val defaultPropertyMap = PropertyMap.mapByType(defaultProperties)

        val title = defaultPropertyMap.get(PropertyType.TITLE)
        val userName = defaultPropertyMap.get(PropertyType.USER_NAME)
        val password = defaultPropertyMap.get(PropertyType.PASSWORD)
        val url = defaultPropertyMap.get(PropertyType.URL)
        val notes = defaultPropertyMap.get(PropertyType.NOTES)

        val properties = mutableListOf<Property>()

        properties.apply {
            add(
                Property(
                    type = PropertyType.TITLE,
                    name = title?.name ?: PropertyType.TITLE.propertyName,
                    value = title?.value ?: EMPTY,
                    isProtected = title?.isProtected ?: false
                )
            )
            add(
                Property(
                    type = PropertyType.USER_NAME,
                    name = userName?.name ?: PropertyType.USER_NAME.propertyName,
                    value = userName?.value ?: EMPTY,
                    isProtected = userName?.isProtected ?: false
                )
            )
            add(
                Property(
                    type = PropertyType.PASSWORD,
                    name = password?.name ?: PropertyType.PASSWORD.propertyName,
                    value = password?.value ?: EMPTY,
                    isProtected = password?.isProtected ?: true
                )
            )
            add(
                Property(
                    type = PropertyType.URL,
                    name = url?.name ?: PropertyType.URL.propertyName,
                    value = url?.value ?: EMPTY,
                    isProtected = url?.isProtected ?: false
                )
            )
            add(
                Property(
                    type = PropertyType.NOTES,
                    name = notes?.name ?: PropertyType.NOTES.propertyName,
                    value = notes?.value ?: EMPTY,
                    isProtected = notes?.isProtected ?: false
                )
            )
        }

        if (customProperties.isNotEmpty()) {
            properties.addAll(customProperties)
        }

        return properties
    }

    private fun isAllDataValid(): Boolean {
        return getPropertyViewModels()
            .all { it.isDataValid() }
    }

    private fun displayError() {
        getPropertyViewModels()
            .onEach {
                if (!it.isDataValid()) {
                    it.displayError()
                }
            }
    }

    private fun getViewModelsWithoutSpace(): List<BaseCellViewModel> {
        return cellViewModels.value
            ?.filter { it !is SpaceCellViewModel }
            ?: emptyList()
    }

    private fun getPropertyViewModels(): List<PropertyViewModel> {
        return cellViewModels.value
            ?.mapNotNull { it as? PropertyViewModel }
            ?: emptyList()
    }

    private fun findViewModelByCellId(cellId: String): BaseCellViewModel? {
        return cellViewModels.value
            ?.mapNotNull { it as? BaseCellViewModel }
            ?.firstOrNull { it.model.id == cellId }
    }

    private fun createNewNoteFromCells(
        groupUid: UUID,
        template: Template?
    ): Note {
        val properties = createPropertiesFromCells().toMutableList()

        val title = PropertyFilter.filterTitle(properties)
            ?.value
            ?: EMPTY

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

    private fun createModifiedNoteFromCells(
        sourceNote: Note,
        sourceTemplate: Template?
    ): Note {
        val modifiedProperties = createPropertiesFromCells().toMutableList()
        val title = PropertyFilter.filterTitle(modifiedProperties)
        val modified = Date(System.currentTimeMillis())

        val hiddenProperties = PropertyFilter.filterHidden(sourceNote.properties)
        val newProperties = (hiddenProperties + modifiedProperties).toMutableList()

        if (sourceTemplate != null && PropertyFilter.filterTemplateUid(hiddenProperties) == null) {
            newProperties.add(
                Property(
                    null,
                    Property.PROPERTY_NAME_TEMPLATE_UID,
                    sourceTemplate.uid.toCleanString()
                )
            )
        }

        return Note(
            uid = sourceNote.uid,
            groupUid = sourceNote.groupUid,
            created = sourceNote.created,
            modified = modified,
            title = title?.value ?: EMPTY,
            properties = newProperties
        )
    }

    private fun isNoteChanged(existingNote: Note, modifiedNote: Note): Boolean {
        return !noteDiffer.isEqualsByFields(
            existingNote,
            modifiedNote,
            NoteDiffer.ALL_FIELDS_WITHOUT_MODIFIED
        )
    }

    private fun isAllDefaultAndEmpty(properties: List<Property>): Boolean {
        return properties.all { property ->
            property.isDefault && property.value.isNullOrEmpty()
        }
    }

    private fun determineCellTypesToAdd(): List<PropertyType> {
        val titleCell = findViewModelByCellId(CellId.TITLE)
        val passwordCell = findViewModelByCellId(CellId.PASSWORD)
        val userNameCell = findViewModelByCellId(CellId.USER_NAME)
        val urlCell = findViewModelByCellId(CellId.URL)
        val notesCell = findViewModelByCellId(CellId.NOTES)

        val cells = mutableListOf<PropertyType>()

        if (titleCell == null) {
            cells.add(PropertyType.TITLE)
        }
        if (passwordCell == null) {
            cells.add(PropertyType.PASSWORD)
        }
        if (userNameCell == null) {
            cells.add(PropertyType.USER_NAME)
        }
        if (urlCell == null) {
            cells.add(PropertyType.URL)
        }
        if (notesCell == null) {
            cells.add(PropertyType.NOTES)
        }

        return cells
    }

    object CellId {
        const val TITLE = "title"
        const val USER_NAME = "userName"
        const val URL = "url"
        const val NOTES = "notes"
        const val PASSWORD = "password"
    }

    enum class AddPropertyDialogItem(val title: String, val propertyType: PropertyType?) {
        TITLE(CellId.TITLE, PropertyType.TITLE),
        PASSWORD(CellId.PASSWORD, PropertyType.PASSWORD),
        USER_NAME(CellId.USER_NAME, PropertyType.USER_NAME),
        URL(CellId.URL, PropertyType.URL),
        NOTES(CellId.NOTES, PropertyType.NOTES),
        CUSTOM("Custom", null)
    }
}