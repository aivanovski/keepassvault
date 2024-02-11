package com.ivanovsky.passnotes.presentation.noteEditor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.entity.PropertyMap
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.noteEditor.NoteEditorInteractor
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.PasswordGeneratorScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.HeaderCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.AttachmentCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.ExtendedTextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.PropertyViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.SecretPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.TextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toCleanString
import com.ivanovsky.passnotes.util.toUUID
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteEditorViewModel(
    private val interactor: NoteEditorInteractor,
    private val resources: ResourceProvider,
    lockInteractor: DatabaseLockInteractor,
    private val errorInteractor: ErrorInteractor,
    private val noteDiffer: NoteDiffer,
    private val dispatchers: DispatcherProvider,
    private val modelFactory: NoteEditorCellModelFactory,
    private val viewModelFactory: NoteEditorCellViewModelFactory,
    observerBus: ObserverBus,
    private val router: Router,
    private val args: NoteEditorArgs
) : BaseScreenViewModel() {

    val viewTypes = ViewModelTypes()
        .add(TextPropertyCellViewModel::class, R.layout.cell_text_property)
        .add(SecretPropertyCellViewModel::class, R.layout.cell_secret_property)
        .add(ExtendedTextPropertyCellViewModel::class, R.layout.cell_extended_text_property)
        .add(SpaceCellViewModel::class, R.layout.cell_space)
        .add(AttachmentCellViewModel::class, R.layout.cell_editable_attachment)
        .add(HeaderCellViewModel::class, R.layout.cell_header)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val isDoneButtonVisible = MutableLiveData<Boolean>()
    val showDiscardDialogEvent = SingleLiveEvent<String>()
    val showAddDialogEvent = SingleLiveEvent<List<Pair<AddDialogItem, String>>>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val showToastEvent = SingleLiveEvent<String>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    private var note: Note? = null
    private var template: Template? = args.template
    private val attachmentMap = mutableMapOf<String, Attachment>()

    init {
        subscribeToEvents()
    }

    fun start() {
        val currentScreenState = screenState.value ?: return
        if (!currentScreenState.isNotInitialized) {
            return
        }

        if (args.mode == NoteEditorMode.NEW) {
            val models = when {
                args.template != null -> modelFactory.createModelsFromTemplate(args.template)
                args.properties != null -> modelFactory.createModelsFromProperties(args.properties)
                else -> modelFactory.createDefaultModels()
            }
            val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
            setCellElements(viewModels)

            isDoneButtonVisible.value = true
            screenState.value = ScreenState.data()
        } else if (args.mode == NoteEditorMode.EDIT) {
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

        if (args.mode == NoteEditorMode.NEW) {
            val groupUid = args.groupUid ?: return

            val note = createNewNoteFromCells(groupUid, template)
            isDoneButtonVisible.value = false
            hideKeyboardEvent.call(Unit)
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
        } else if (args.mode == NoteEditorMode.EDIT) {
            val sourceNote = note ?: return
            val sourceTemplate = template

            isDoneButtonVisible.value = false
            hideKeyboardEvent.call(Unit)
            screenState.value = ScreenState.loading()

            val newNote = createModifiedNoteFromCells(sourceNote, sourceTemplate)
            if (isNoteChanged(sourceNote, newNote)) {
                viewModelScope.launch {
                    val updateNoteResult = interactor.updateNote(newNote)

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

        when (args.mode) {
            NoteEditorMode.NEW -> {
                if (properties.isEmpty() || isAllEmpty(properties)) {
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

        val dialogItems = mutableListOf<Pair<AddDialogItem, String>>()

        for (propertyType in typesToAdd) {
            val item = when (propertyType) {
                PropertyType.TITLE -> {
                    Pair(
                        AddDialogItem.TITLE,
                        resources.getString(R.string.title)
                    )
                }

                PropertyType.PASSWORD -> {
                    Pair(
                        AddDialogItem.PASSWORD,
                        resources.getString(R.string.password)
                    )
                }

                PropertyType.USER_NAME -> {
                    Pair(
                        AddDialogItem.USER_NAME,
                        resources.getString(R.string.username)
                    )
                }

                PropertyType.URL -> {
                    Pair(
                        AddDialogItem.URL,
                        resources.getString(R.string.url_cap)
                    )
                }

                PropertyType.NOTES -> {
                    Pair(
                        AddDialogItem.NOTES,
                        resources.getString(R.string.notes)
                    )
                }

                else -> throw IllegalStateException()
            }

            dialogItems.add(item)
        }

        dialogItems.add(
            Pair(
                AddDialogItem.CUSTOM_PROPERTY,
                resources.getString(R.string.custom_property)
            )
        )
        dialogItems.add(
            Pair(
                AddDialogItem.ATTACHMENT,
                resources.getString(R.string.attachment)
            )
        )

        showAddDialogEvent.call(dialogItems)
    }

    fun onAddDialogItemSelected(item: AddDialogItem) {
        if (item == AddDialogItem.ATTACHMENT) {
            router.setResultListener(StorageListScreen.RESULT_KEY) { file ->
                if (file is FileDescriptor) {
                    onFileAttached(file)
                }
            }
            router.navigateTo(
                StorageListScreen(
                    args = StorageListArgs(
                        action = Action.PICK_FILE
                    )
                )
            )
        } else {
            val newModel = modelFactory.createCustomPropertyModel(item.propertyType)
            val newViewModel = viewModelFactory.createCellViewModel(newModel, eventProvider)

            setCellElements(insertPropertyCell(getViewModels(), newViewModel))
        }
    }

    private fun onFileAttached(file: FileDescriptor) {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val createAttachmentResult = interactor.createAttachment(
                file = file,
                currentAttachments = attachmentMap.values
            )

            if (createAttachmentResult.isSucceededOrDeferred) {
                val attachment = createAttachmentResult.obj

                attachmentMap[attachment.uid] = attachment

                val model = modelFactory.createAttachmentCell(attachment)
                val viewModel = viewModelFactory.createCellViewModel(model, eventProvider)

                screenState.value = ScreenState.data()
                setCellElements(insertAttachmentCell(getViewModels(), viewModel))
            } else {
                val message = errorInteractor.processAndGetMessage(createAttachmentResult.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    private fun finishScreen() {
        router.backTo(
            GroupsScreen(
                GroupsScreenArgs(
                    appMode = ApplicationLaunchMode.NORMAL,
                    groupUid = args.groupUid,
                    isCloseDatabaseOnExit = (args.groupUid == null),
                    isSearchModeEnabled = false
                )
            )
        )
    }

    private fun loadData() {
        val uid = args.noteUid ?: return

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val noteResult = withContext(dispatchers.IO) {
                interactor.loadNote(uid)
            }

            if (noteResult.isSucceededOrDeferred) {
                val note = noteResult.obj
                val template = loadTemplate(note)

                onDataLoaded(note, template)

                isDoneButtonVisible.value = true
                screenState.value = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(noteResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun onDataLoaded(note: Note, template: Template?) {
        this.note = note
        this.template = template

        attachmentMap.clear()
        for (attachment in note.attachments) {
            attachmentMap[attachment.uid] = attachment
        }

        val models = modelFactory.createModelsForNote(note, template)
        val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
        setCellElements(viewModels)
    }

    private suspend fun loadTemplate(note: Note): Template? =
        withContext(dispatchers.IO) {
            val filter = PropertyFilter.Builder()
                .filterTemplateUid()
                .build()

            val templateUid = filter.apply(note.properties)
                .firstOrNull()
                ?.value
                ?.toUUID()

            if (templateUid != null) {
                val loadTemplateResult = interactor.loadTemplate(templateUid)
                loadTemplateResult.obj
            } else {
                null
            }
        }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(ExtendedTextPropertyCellViewModel.REMOVE_EVENT) -> {
                    val cellId = event.getString(ExtendedTextPropertyCellViewModel.REMOVE_EVENT)
                    if (cellId != null) {
                        setCellElements(removeCellById(getViewModels(), cellId))
                    }
                }

                event.containsKey(SecretPropertyCellViewModel.GENERATE_CLICK_EVENT) -> {
                    val cellId = event.getString(SecretPropertyCellViewModel.GENERATE_CLICK_EVENT)
                    if (cellId != null) {
                        router.setResultListener(PasswordGeneratorScreen.RESULT_KEY) { password ->
                            if (password is String) {
                                setPasswordToCell(cellId, password)
                            }
                        }
                        router.navigateTo(PasswordGeneratorScreen())
                    }
                }

                event.containsKey(AttachmentCellViewModel.REMOVE_ICON_CLICK_EVENT) -> {
                    val attachmentUid =
                        event.getString(AttachmentCellViewModel.REMOVE_ICON_CLICK_EVENT)
                    if (attachmentUid != null) {
                        onRemoveAttachmentButtonClicked(attachmentUid)
                    }
                }
            }
        }
    }

    private fun onRemoveAttachmentButtonClicked(uid: String) {
        attachmentMap.remove(uid)

        setCellElements(removeAttachmentCell(getViewModels(), cellId = uid))
    }

    private fun setPasswordToCell(cellId: String, password: String) {
        val viewModel = (findViewModelByCellId(cellId) as? SecretPropertyCellViewModel) ?: return

        val newModel = viewModel.model.copy(value = password)

        updateCellElement(cellId, viewModelFactory.createCellViewModel(newModel, eventProvider))
    }

    private fun updateCellElement(cellId: String, viewModel: BaseCellViewModel) {
        val viewModels = cellViewModels.value?.toMutableList() ?: return

        val idx = viewModels.indexOfFirst { it.model.id == cellId }
        if (idx == -1) {
            return
        }

        viewModels[idx] = viewModel

        setCellElements(viewModels)
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

    private fun createAttachmentsFromCells(): List<Attachment> {
        val viewModels = filterAttachmentViewModels(getViewModels())

        return viewModels.mapNotNull { viewModel -> attachmentMap[viewModel.model.id] }
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

    private fun insertPropertyCell(
        viewModels: MutableList<BaseCellViewModel>,
        newViewModel: BaseCellViewModel
    ): MutableList<BaseCellViewModel> {
        val insertIdx = viewModels.indexOfLast { viewModel -> viewModel is PropertyViewModel }
        if (insertIdx != viewModels.lastIndex) {
            viewModels.add(insertIdx + 1, newViewModel)
        } else {
            viewModels.add(newViewModel)
        }

        return viewModels
    }

    private fun insertAttachmentCell(
        viewModels: MutableList<BaseCellViewModel>,
        newViewModel: BaseCellViewModel
    ): MutableList<BaseCellViewModel> {
        var insertIdx = viewModels.indexOfLast { viewModel -> viewModel is AttachmentCellViewModel }
        if (insertIdx == -1) {
            val headerModel = modelFactory.createAttachmentHeaderCell()
            val headerViewModel = viewModelFactory.createCellViewModel(headerModel, eventProvider)

            insertIdx = viewModels.indexOfLast { viewModel -> viewModel is PropertyViewModel }

            viewModels.add(insertIdx + 1, headerViewModel)
            viewModels.add(insertIdx + 2, newViewModel)
        } else {
            viewModels.add(insertIdx + 1, newViewModel)
        }

        return viewModels
    }

    private fun removeAttachmentCell(
        viewModels: MutableList<BaseCellViewModel>,
        cellId: String
    ): MutableList<BaseCellViewModel> {
        removeCellById(viewModels, cellId)

        val attachmentCount = viewModels.count { viewModel -> viewModel is AttachmentCellViewModel }
        if (attachmentCount == 0) {
            removeCellById(viewModels, CellId.ATTACHMENT_HEADER)
        }

        return viewModels
    }

    private fun removeCellById(
        viewModels: MutableList<BaseCellViewModel>,
        cellId: String
    ): MutableList<BaseCellViewModel> {
        viewModels.removeIf { viewModel -> viewModel.model.id == cellId }
        return viewModels
    }

    private fun getViewModels(): MutableList<BaseCellViewModel> {
        return cellViewModels.value?.toMutableList() ?: mutableListOf()
    }

    private fun getPropertyViewModels(): List<PropertyViewModel> {
        return cellViewModels.value
            ?.mapNotNull { it as? PropertyViewModel }
            ?: emptyList()
    }

    private fun filterAttachmentViewModels(
        viewModels: List<BaseCellViewModel>
    ): List<AttachmentCellViewModel> {
        return viewModels.mapNotNull { it as? AttachmentCellViewModel }
    }

    private fun findViewModelByCellId(cellId: String): BaseCellViewModel? {
        return cellViewModels.value
            ?.firstOrNull { it.model.id == cellId }
    }

    private fun createNewNoteFromCells(
        groupUid: UUID,
        template: Template?
    ): Note {
        val properties = createPropertiesFromCells().toMutableList()
        val attachments = createAttachmentsFromCells()

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

        return Note(null, groupUid, created, created, title, properties, attachments)
    }

    private fun createModifiedNoteFromCells(
        sourceNote: Note,
        sourceTemplate: Template?
    ): Note {
        val modifiedProperties = createPropertiesFromCells().toMutableList()
        val attachments = createAttachmentsFromCells()
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
            properties = newProperties,
            attachments = attachments
        )
    }

    private fun isNoteChanged(existingNote: Note, modifiedNote: Note): Boolean {
        return !noteDiffer.isEqualsByFields(
            existingNote,
            modifiedNote,
            NoteDiffer.ALL_FIELDS_WITHOUT_MODIFIED
        )
    }

    private fun isAllEmpty(properties: List<Property>): Boolean {
        return properties.all { property -> property.value.isNullOrEmpty() }
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
        const val ATTACHMENT_HEADER = "attachment_header"
    }

    enum class AddDialogItem(val propertyType: PropertyType?) {
        TITLE(PropertyType.TITLE),
        PASSWORD(PropertyType.PASSWORD),
        USER_NAME(PropertyType.USER_NAME),
        URL(PropertyType.URL),
        NOTES(PropertyType.NOTES),
        CUSTOM_PROPERTY(null),
        ATTACHMENT(null)
    }
}