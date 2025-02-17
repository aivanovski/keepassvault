package com.ivanovsky.passnotes.presentation.diffViewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.EnterDbCredentialsScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenVisibilityHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffFilesCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffFilesCellViewModel.Companion.CHANGE_BUTTON_CLICK_EVENT
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffHeaderCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.factory.DiffViewerCellModelFactory
import com.ivanovsky.passnotes.presentation.diffViewer.factory.DiffViewerCellViewModelFactory
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffEntity
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsScreenArgs
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.lang.IllegalStateException
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class DiffViewerViewModel(
    private val interactor: DiffViewerInteractor,
    private val viewModelFactory: DiffViewerCellViewModelFactory,
    private val modelFactory: DiffViewerCellModelFactory,
    private val router: Router,
    private val resourceProvider: ResourceProvider,
    private val args: DiffViewerScreenArgs
) : BaseScreenViewModel(
    initialState = ScreenState.loading()
) {

    val viewTypes = ViewModelTypes()
        .add(SpaceCellViewModel::class, R.layout.cell_space)
        .add(DiffFilesCellViewModel::class, R.layout.cell_diff_files)
        .add(DiffHeaderCellViewModel::class, R.layout.cell_diff_header)
        .add(DiffCellViewModel::class, R.layout.cell_diff)

    val screenStateHandler = DefaultScreenVisibilityHandler()
    val leftFilename = MutableLiveData(EMPTY)
    val rightFilename = MutableLiveData(EMPTY)
    val isButtonsVisible = MutableLiveData(false)
    val isLeftButtonEnabled = MutableLiveData(true)
    val isRightButtonEnabled = MutableLiveData(true)
    val isCompareButtonEnabled = MutableLiveData(false)

    private var leftData: Pair<KotpassDatabase, FileDescriptor>? = null
    private var rightData: Pair<KotpassDatabase, FileDescriptor>? = null
    private var diff: List<DiffListItem> = emptyList()
    private val collapsedEventIds = mutableSetOf<Int>()

    private var leftEntity: DiffEntity = args.left
    private var rightEntity: DiffEntity = args.right

    init {
        subscribeToCellEvents()
    }

    fun start() {
        if (isArgumentsRequireSelection()) {
            isLeftButtonEnabled.value = (leftEntity != DiffEntity.OpenedDatabase)
            isRightButtonEnabled.value = (rightEntity != DiffEntity.OpenedDatabase)

            leftFilename.value = leftEntity.toReadableString()
            rightFilename.value = rightEntity.toReadableString()

            isButtonsVisible.value = true
            isCompareButtonEnabled.value = isAllEntitiesSpecified()

            setScreenState(ScreenState.data())
        } else {
            readDatabases()
        }
    }

    fun navigateBack() {
        router.exit()
    }

    fun onSelectLeftFileClicked() {
        navigateToFilePicker { key, file ->
            onLeftFileSelected(key, file)
        }
    }

    fun onSelectRightFileClicked() {
        navigateToFilePicker { key, file ->
            onRightFileSelected(key, file)
        }
    }

    fun onChangeButtonClicked() {
        invert(isReloadData = false)
    }

    fun onCompareButtonClicked() {
        if (!isAllEntitiesSpecified()) {
            return
        }

        readDatabases()
    }

    private fun navigateToFilePicker(
        onFileSelected: (key: EncryptedDatabaseKey, file: FileDescriptor) -> Unit
    ) {
        val resultKey = StorageListScreen.newResultKey()

        router.setResultListener(resultKey) { file ->
            if (file !is FileDescriptor) {
                return@setResultListener
            }

            router.setResultListener(EnterDbCredentialsScreen.RESULT_KEY) { key ->
                if (key is EncryptedDatabaseKey) {
                    onFileSelected.invoke(key, file)
                }
            }

            router.navigateTo(
                EnterDbCredentialsScreen(
                    EnterDbCredentialsScreenArgs(
                        file = file
                    )
                )
            )
        }

        router.navigateTo(
            StorageListScreen(
                args = StorageListArgs(
                    action = Action.PICK_FILE,
                    resultKey = resultKey
                )
            )
        )
    }

    private fun onLeftFileSelected(
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ) {
        val entity = DiffEntity.File(key, file)

        leftEntity = entity
        leftFilename.value = entity.toReadableString()
        isCompareButtonEnabled.value = isAllEntitiesSpecified()
    }

    private fun onRightFileSelected(
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ) {
        val entity = DiffEntity.File(key, file)

        rightEntity = entity
        rightFilename.value = entity.toReadableString()
        isCompareButtonEnabled.value = isAllEntitiesSpecified()
    }

    private fun subscribeToCellEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(CHANGE_BUTTON_CLICK_EVENT) -> {
                    onChangeFilesClicked()
                }

                event.containsKey(DiffCellViewModel.CLICK_EVENT) -> {
                    val eventId = event.getInt(DiffCellViewModel.CLICK_EVENT) as Int
                    onCellClicked(eventId)
                }
            }
        }
    }

    private fun onChangeFilesClicked() {
        invert(isReloadData = true)
    }

    private fun invert(isReloadData: Boolean) {
        val oldLeftEntity = leftEntity
        val oldRightEntity = rightEntity

        val oldLeftData = leftData
        val oldRightData = rightData

        leftEntity = oldRightEntity
        rightEntity = oldLeftEntity

        leftData = oldRightData
        rightData = oldLeftData

        leftFilename.value = leftEntity.toReadableString()
        rightFilename.value = rightEntity.toReadableString()

        collapsedEventIds.clear()

        if (isReloadData) {
            if (canDoDiff()) {
                loadDiff()
            } else if (isAllEntitiesSpecified()) {
                readDatabases()
            }
        }
    }

    private fun onCellClicked(eventId: Int) {
        if (eventId in collapsedEventIds) {
            collapsedEventIds.remove(eventId)
        } else {
            collapsedEventIds.add(eventId)
        }

        val (_, leftFile) = leftData ?: return
        val (_, rightFile) = rightData ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            onDiffLoaded(
                leftFile = leftFile,
                rightFile = rightFile,
                diff = diff,
                collapsedEventIds = collapsedEventIds
            )
        }
    }

    private fun readDatabases() {
        val leftEntity = this.leftEntity
        val rightEntity = this.rightEntity

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val leftDataResult = loadDatabaseAndFile(leftEntity)
            if (leftDataResult.isFailed) {
                setErrorState(leftDataResult.error)
                return@launch
            }

            val rightDataResult = loadDatabaseAndFile(rightEntity)
            if (rightDataResult.isFailed) {
                setErrorState(rightDataResult.error)
                return@launch
            }

            leftData = leftDataResult.getOrThrow()
            rightData = rightDataResult.getOrThrow()

            loadDiff()
        }
    }

    private suspend fun loadDatabaseAndFile(
        entity: DiffEntity
    ): OperationResult<Pair<KotpassDatabase, FileDescriptor>> {
        return when (entity) {
            is DiffEntity.OpenedDatabase -> interactor.getOpenedDatabaseAndFile()

            is DiffEntity.File -> {
                val readDatabaseResult = interactor.readDatabase(entity.key, entity.file)
                if (readDatabaseResult.isFailed) {
                    return readDatabaseResult.mapError()
                }

                val db = readDatabaseResult.getOrThrow()
                OperationResult.success(db to db.file)
            }

            else -> throw IllegalStateException()
        }
    }

    private fun loadDiff() {
        val (leftDb, leftFile) = leftData ?: return
        val (rightDb, rightFile) = rightData ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val getDiffResult = interactor.getDiff(leftDb, rightDb)
            if (getDiffResult.isFailed) {
                setErrorState(getDiffResult.error)
                return@launch
            }

            onDiffLoaded(
                leftFile = leftFile,
                rightFile = rightFile,
                diff = getDiffResult.getOrThrow(),
                collapsedEventIds = collapsedEventIds
            )
        }
    }

    private fun onDiffLoaded(
        leftFile: FileDescriptor,
        rightFile: FileDescriptor,
        diff: List<DiffListItem>,
        collapsedEventIds: Set<Int>
    ) {
        this.diff = diff

        isButtonsVisible.value = false

        if (diff.isNotEmpty()) {
            val viewModels = viewModelFactory.createCellViewModels(
                models = modelFactory.createDiffModels(
                    leftName = leftFile.name,
                    leftTime = leftFile.modified,
                    rightName = rightFile.name,
                    rightTime = rightFile.modified,
                    diff = diff,
                    collapsedEventIds = collapsedEventIds
                ),
                eventProvider = eventProvider
            )

            setCellViewModels(viewModels)
            setScreenState(ScreenState.data())
        } else {
            setScreenState(
                ScreenState.empty(
                    emptyText = resourceProvider.getString(R.string.files_are_identical)
                )
            )
        }
    }

    private fun isArgumentsRequireSelection(): Boolean {
        return args.left == DiffEntity.SelectFile || args.right == DiffEntity.SelectFile
    }

    private fun isAllEntitiesSpecified(): Boolean {
        return leftEntity !is DiffEntity.SelectFile && rightEntity !is DiffEntity.SelectFile
    }

    private fun canDoDiff(): Boolean {
        return leftData != null && rightData != null
    }

    private fun DiffEntity.toReadableString(): String {
        return when (this) {
            is DiffEntity.OpenedDatabase -> resourceProvider.getString(R.string.opened_database)
            is DiffEntity.SelectFile -> resourceProvider.getString(R.string.pick_file)
            is DiffEntity.File -> file.name
        }
    }

    class Factory(private val args: DiffViewerScreenArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<DiffViewerViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}