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
import com.ivanovsky.passnotes.domain.entity.DateData
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.entity.PropertyMap
import com.ivanovsky.passnotes.domain.entity.TimeData
import com.ivanovsky.passnotes.domain.entity.Timestamp
import com.ivanovsky.passnotes.domain.entity.Timestamp.Companion.currentTimestamp
import com.ivanovsky.passnotes.domain.entity.TimestampBuilder
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.noteEditor.NoteEditorInteractor
import com.ivanovsky.passnotes.domain.otp.OtpUriFactory
import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.PasswordGeneratorScreen
import com.ivanovsky.passnotes.presentation.Screens.SetupOneTimePasswordScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenVisibilityHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.HeaderCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.AttachmentCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.ExpirationCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.ExtendedTextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.PropertyViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.SecretPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.TextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellModelFactory.ExpirationData
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.SetupOneTimePasswordArgs
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.TimeUtils.combine
import com.ivanovsky.passnotes.util.TimeUtils.toDate
import com.ivanovsky.passnotes.util.TimeUtils.toJavaDate
import com.ivanovsky.passnotes.util.TimeUtils.toTime
import com.ivanovsky.passnotes.util.TimeUtils.toTimestamp
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
        .add(ExpirationCellViewModel::class, R.layout.cell_expiration_property)

    val screenStateHandler = DefaultScreenVisibilityHandler()
    val isDoneButtonVisible = MutableLiveData<Boolean>()
    val showDiscardDialogEvent = SingleLiveEvent<String>()
    val showAddDialogEvent = SingleLiveEvent<List<Pair<CellType, String>>>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val showToastEvent = SingleLiveEvent<String>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)
    val showDatePickerEvent = SingleLiveEvent<DateData>()
    val showTimePickerEvent = SingleLiveEvent<TimeData>()

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
            val expirationData = ExpirationData(
                isEnabled = false,
                timestamp = createDefaultExpiration()
            )

            val models = when {
                args.template != null -> modelFactory.createModelsFromTemplate(args.template)
                args.properties != null -> {
                    modelFactory.createModelsFromProperties(
                        properties = args.properties,
                        expiration = expirationData
                    )
                }

                else -> modelFactory.createDefaultModels(
                    expiration = expirationData
                )
            }
            val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
            setCellViewModels(viewModels)

            setScreenState(ScreenState.data())
        } else if (args.mode == NoteEditorMode.EDIT) {
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
            hideKeyboardEvent.call(Unit)
            setScreenState(ScreenState.loading())

            viewModelScope.launch {
                val createNoteResult = withContext(Dispatchers.Default) {
                    interactor.createNewNote(note)
                }

                if (createNoteResult.isSucceededOrDeferred) {
                    finishScreen()
                } else {
                    setErrorPanelState(createNoteResult.error)
                }
            }
        } else if (args.mode == NoteEditorMode.EDIT) {
            val sourceNote = note ?: return
            val sourceTemplate = template

            hideKeyboardEvent.call(Unit)
            setScreenState(ScreenState.loading())

            val newNote = createModifiedNoteFromCells(sourceNote, sourceTemplate)
            if (isNoteChanged(sourceNote, newNote)) {
                viewModelScope.launch {
                    val updateNoteResult = interactor.updateNote(newNote)

                    if (updateNoteResult.isSucceededOrDeferred) {
                        finishScreen()
                    } else {
                        setErrorPanelState(updateNoteResult.error)
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

        val dialogItems = typesToAdd.map { cellType ->
            val name = when (cellType) {
                is CellType.Title -> resources.getString(R.string.title)
                is CellType.Password -> resources.getString(R.string.password)
                is CellType.UserName -> resources.getString(R.string.username)
                is CellType.Expiration -> resources.getString(R.string.expiration)
                is CellType.Url -> resources.getString(R.string.url_cap)
                is CellType.Notes -> resources.getString(R.string.notes)
                is CellType.OTP -> resources.getString(R.string.one_time_password)
                is CellType.Custom -> resources.getString(R.string.custom_property)
                is CellType.Attachment -> resources.getString(R.string.attachment)
            }

            cellType to name
        }

        showAddDialogEvent.call(dialogItems)
    }

    fun onAddDialogItemSelected(cellType: CellType) {
        when (cellType) {
            is CellType.OTP -> {
                router.setResultListener(SetupOneTimePasswordScreen.RESULT_KEY) { token ->
                    if (token is OtpToken) {
                        onOtpTokenCreated(token)
                    }
                }
                router.navigateTo(
                    SetupOneTimePasswordScreen(
                        SetupOneTimePasswordArgs(
                            tokenIssuer = resources.getString(R.string.app_name).lowercase()
                        )
                    )
                )
            }

            is CellType.Attachment -> {
                val resultKey = StorageListScreen.newResultKey()

                router.setResultListener(resultKey) { file ->
                    if (file is FileDescriptor) {
                        onFileAttached(file)
                    }
                }

                router.navigateTo(
                    StorageListScreen(
                        StorageListArgs(
                            action = Action.PICK_FILE,
                            resultKey = resultKey
                        )
                    )
                )
            }

            is CellType.Expiration -> {
                val newModel = modelFactory.createExpirationCell(
                    expiration = ExpirationData(
                        isEnabled = true,
                        timestamp = createDefaultExpiration()
                    )
                )
                val newViewModel = viewModelFactory.createCellViewModel(newModel, eventProvider)

                val urlCellIndex = getViewModels().indexOfFirst { viewModel ->
                    viewModel.model.id == CellId.URL
                }

                val viewModels = if (urlCellIndex > 0) {
                    insertPropertyCell(getViewModels(), newViewModel, urlCellIndex - 1)
                } else {
                    insertPropertyCell(getViewModels(), newViewModel)
                }

                setCellViewModels(viewModels)
            }

            else -> {
                val newModel = modelFactory.createCustomPropertyModel(cellType.getPropertyType())
                val newViewModel = viewModelFactory.createCellViewModel(newModel, eventProvider)

                setCellViewModels(insertPropertyCell(getViewModels(), newViewModel))
            }
        }
    }

    fun onExpirationDateChanged(date: DateData) {
        val currentExpiration = getExpirationFromCell() ?: return

        val newExpiration = combine(date, currentExpiration.toTime())

        setExpirationToCell(newExpiration)
    }

    fun onExpirationTimeChanged(time: TimeData) {
        val currentExpiration = getExpirationFromCell() ?: return

        val newExpiration = combine(currentExpiration.toDate(), time)

        setExpirationToCell(newExpiration)
    }

    private fun onOtpTokenCreated(token: OtpToken) {
        val url = OtpUriFactory.createUri(token)

        val otpModel = modelFactory.createOtpCell(url)
        val otpViewModel = viewModelFactory.createCellViewModel(otpModel, eventProvider)

        setCellViewModels(insertPropertyCell(getViewModels(), otpViewModel))
    }

    private fun onFileAttached(file: FileDescriptor) {
        setScreenState(ScreenState.loading())

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

                setScreenState(ScreenState.data())
                setCellViewModels(insertAttachmentCell(getViewModels(), viewModel))
            } else {
                setErrorPanelState(createAttachmentResult.error)
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

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val noteResult = withContext(dispatchers.IO) {
                interactor.loadNote(uid)
            }

            if (noteResult.isSucceededOrDeferred) {
                val note = noteResult.obj
                val template = loadTemplate(note)

                onDataLoaded(note, template)

                setScreenState(ScreenState.data())
            } else {
                setErrorPanelState(noteResult.error)
            }
        }
    }

    private fun onDataLoaded(note: Note, template: Template?) {
        this.note = note
        this.template = template

        val isExpirationEnabled = (note.expiration != null)
        val expiration = note.expiration?.time?.toTimestamp()
            ?: createDefaultExpiration()

        attachmentMap.clear()
        for (attachment in note.attachments) {
            attachmentMap[attachment.uid] = attachment
        }

        val models = modelFactory.createModelsForNote(
            note,
            template,
            ExpirationData(isExpirationEnabled, expiration)
        )
        val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)
        setCellViewModels(viewModels)
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
                        setCellViewModels(removeCellById(getViewModels(), cellId))
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

                event.containsKey(ExpirationCellViewModel.DATE_CLICK_EVENT) -> {
                    onExpirationDateClicked()
                }

                event.containsKey(ExpirationCellViewModel.TIME_CLICK_EVENT) -> {
                    onExpirationTimeClicked()
                }
            }
        }
    }

    private fun onRemoveAttachmentButtonClicked(uid: String) {
        attachmentMap.remove(uid)

        setCellViewModels(removeAttachmentCell(getViewModels(), cellId = uid))
    }

    private fun onExpirationDateClicked() {
        val date = getExpirationFromCell()?.toDate() ?: return

        showDatePickerEvent.call(date)
    }

    private fun onExpirationTimeClicked() {
        val time = getExpirationFromCell()?.toTime() ?: return

        showTimePickerEvent.call(time)
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

        setCellViewModels(viewModels)
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
        val otp = defaultPropertyMap.get(PropertyType.OTP)

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

            if (otp != null) {
                add(
                    Property(
                        type = PropertyType.OTP,
                        name = otp.name,
                        value = otp.value,
                        isProtected = otp.isProtected
                    )
                )
            }
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

    private fun getExpirationCellViewModel(): ExpirationCellViewModel? {
        return findViewModelByCellId(CellId.EXPIRATION) as? ExpirationCellViewModel
    }

    private fun getExpirationFromCell(): Timestamp? {
        val model = getExpirationCellViewModel()?.model ?: return null

        return if (model.isEnabled) {
            model.timestamp
        } else {
            null
        }
    }

    private fun setExpirationToCell(expiration: Timestamp) {
        val viewModel = getExpirationCellViewModel() ?: return

        val newModel = viewModel.model.copy(
            timestamp = expiration
        )
        viewModel.setModel(newModel)
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

    private fun insertPropertyCell(
        viewModels: MutableList<BaseCellViewModel>,
        newViewModel: BaseCellViewModel,
        index: Int
    ): MutableList<BaseCellViewModel> {
        viewModels.add(index, newViewModel)
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

    private fun findViewModelByPropertyName(propertyName: String): BaseCellViewModel? {
        return cellViewModels.value
            ?.mapNotNull { viewModel -> viewModel as? ExtendedTextPropertyCellViewModel }
            ?.firstOrNull { viewModel ->
                val (name, _) = viewModel.getNameAndValue()
                name == propertyName
            }
    }

    private fun createNewNoteFromCells(
        groupUid: UUID,
        template: Template?
    ): Note {
        val properties = createPropertiesFromCells().toMutableList()
        val attachments = createAttachmentsFromCells()
        val expiration = getExpirationFromCell()?.toJavaDate()

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

        return Note(
            uid = null,
            groupUid = groupUid,
            created = created,
            modified = created,
            expiration = expiration,
            title = title,
            properties = properties,
            attachments = attachments
        )
    }

    private fun createModifiedNoteFromCells(
        sourceNote: Note,
        sourceTemplate: Template?
    ): Note {
        val modifiedProperties = createPropertiesFromCells().toMutableList()
        val attachments = createAttachmentsFromCells()
        val title = PropertyFilter.filterTitle(modifiedProperties)
        val modified = Date(System.currentTimeMillis())
        val expiration = getExpirationFromCell()?.toJavaDate()

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
            expiration = expiration,
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

    override fun setScreenState(state: ScreenState) {
        super.setScreenState(state)
        isDoneButtonVisible.value = state.isDisplayingData
    }

    private fun isAllEmpty(properties: List<Property>): Boolean {
        return properties.all { property -> property.value.isNullOrEmpty() }
    }

    private fun determineCellTypesToAdd(): List<CellType> {
        val titleCell = findViewModelByCellId(CellId.TITLE)
        val passwordCell = findViewModelByCellId(CellId.PASSWORD)
        val userNameCell = findViewModelByCellId(CellId.USER_NAME)
        val expirationCell = findViewModelByCellId(CellId.EXPIRATION)
        val urlCell = findViewModelByCellId(CellId.URL)
        val notesCell = findViewModelByCellId(CellId.NOTES)
        val otpCell = findViewModelByCellId(CellId.OTP)
            ?: findViewModelByPropertyName(PropertyType.OTP.propertyName)

        val cells = mutableListOf<CellType>()

        if (titleCell == null) cells.add(CellType.Title)
        if (passwordCell == null) cells.add(CellType.Password)
        if (userNameCell == null) cells.add(CellType.UserName)
        if (urlCell == null) cells.add(CellType.Url)
        if (expirationCell == null) cells.add(CellType.Expiration)
        if (notesCell == null) cells.add(CellType.Notes)
        if (otpCell == null) cells.add(CellType.OTP)

        cells.add(CellType.Custom)
        cells.add(CellType.Attachment)

        return cells
    }

    private fun CellType.getPropertyType(): PropertyType? {
        return when (this) {
            is CellType.Title -> PropertyType.TITLE
            is CellType.Password -> PropertyType.PASSWORD
            is CellType.UserName -> PropertyType.USER_NAME
            is CellType.Url -> PropertyType.URL
            is CellType.Notes -> PropertyType.NOTES
            is CellType.OTP -> PropertyType.OTP
            else -> null
        }
    }

    private fun createDefaultExpiration(): Timestamp {
        return TimestampBuilder(currentTimestamp())
            .shiftMonth(1)
            .setMinute(0)
            .setSecond(0)
            .build()
    }

    object CellId {
        const val TITLE = "title"
        const val USER_NAME = "userName"
        const val URL = "url"
        const val NOTES = "notes"
        const val PASSWORD = "password"
        const val ATTACHMENT_HEADER = "attachment_header"
        const val OTP = "otp"
        const val EXPIRATION = "expiration"
    }

    sealed interface CellType {
        data object Title : CellType
        data object Password : CellType
        data object UserName : CellType
        data object Url : CellType
        data object Notes : CellType
        data object OTP : CellType
        data object Expiration : CellType
        data object Attachment : CellType
        data object Custom : CellType
    }
}