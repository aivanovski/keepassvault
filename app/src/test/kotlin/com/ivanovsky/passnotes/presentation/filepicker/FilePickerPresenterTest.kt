package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.TEST_DISPATCHER_PROVIDER
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.presentation.core.ScreenState
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.text.SimpleDateFormat

class FilePickerPresenterTest {

    private lateinit var view: FilePickerContract.View
    private lateinit var interactor: FilePickerInteractor
    private lateinit var errorInteractor: ErrorInteractor
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var resources: ResourceProvider
    private lateinit var dateFormatProvider: DateFormatProvider

    @Before
    fun setup() {
        view = mockk(relaxUnitFun = true)
        interactor = mockk(relaxUnitFun = true)
        errorInteractor = mockk(relaxUnitFun = true)
        permissionHelper = mockk(relaxUnitFun = true)
        resources = mockk(relaxUnitFun = true)
        dateFormatProvider = mockk(relaxUnitFun = true)

        val module = module {
            single { interactor }
            single { errorInteractor }
            single { permissionHelper }
            single { resources }
            single { dateFormatProvider }
            single { TEST_DISPATCHER_PROVIDER }
        }

        startKoin {
            printLogger()
            modules(module)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `loadData should request permission`() {
        // arrange
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(false)

        // act
        createPresenter(
            rootFile = ROOT_DIR
        ).loadData()

        // assert
        verify { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { view.requestPermission(PERMISSION) }
        verify { view.setDoneButtonVisibility(false) }
        verify { view.screenState = ScreenState.loading() }
    }

    @Test
    fun `loadData should load files`() {
        // arrange
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(true)
        every { interactor.getFileList(ROOT_DIR) }.returns(OperationResult.success(ROOT_DIR_FILES))
        every { dateFormatProvider.getShortDateFormat() }.returns(DATE_FORMAT)

        // act
        createPresenter(
            rootFile = ROOT_DIR,
            isBrowsingEnabled = true
        ).loadData()

        // asset
        verify { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { interactor.getFileList(ROOT_DIR) }
        verify { view.setDoneButtonVisibility(true) }
        verify { view.screenState = ScreenState.data() }
        verify { view.setItems(ROOT_DIR_VIEW_ITEMS) }
    }

    @Test
    fun `loadData should show empty state`() {
        // arrange
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(true)
        every { interactor.getFileList(ROOT_DIR) }.returns(OperationResult.success(emptyList()))
        every { resources.getString(R.string.no_items) }.returns(EMPTY_TEXT)

        // act
        createPresenter(
            rootFile = ROOT_DIR
        ).loadData()

        // assert
        verify { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { resources.getString(R.string.no_items) }
        verify { interactor.getFileList(ROOT_DIR) }
        verify { view.screenState = ScreenState.empty(EMPTY_TEXT) }
        verify { view.setDoneButtonVisibility(false) }
    }

    @Test
    fun `loadData should show error`() {
        // arrange
        val error = newGenericError(ERROR_TEXT)
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(true)
        every { errorInteractor.processAndGetMessage(error) }.returns(ERROR_TEXT)
        every { interactor.getFileList(ROOT_DIR) }.returns(
            OperationResult.error(newGenericError(ERROR_TEXT))
        )

        // act
        createPresenter(
            rootFile = ROOT_DIR
        ).loadData()

        // assert
        verify { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { errorInteractor.processAndGetMessage(error) }
        verify { interactor.getFileList(ROOT_DIR) }
        verify { view.screenState = ScreenState.error(ERROR_TEXT) }
        verify { view.setDoneButtonVisibility(false) }
    }

    @Test
    fun `onPermissionGranted should show error`() {
        // arrange
        every { resources.getString(R.string.permission_denied_message) }.returns(ERROR_TEXT)

        // act
        createPresenter(
            rootFile = ROOT_DIR
        ).onPermissionResult(false)

        // assert
        verify { view.screenState = ScreenState.error(ERROR_TEXT) }
        verify { view.setDoneButtonVisibility(false) }
    }

    @Test
    fun `onPermissionGranted should load data`() {
        // arrange
        val presenter = spyk(
            createPresenter(
                rootFile = ROOT_DIR
            )
        )
        every { presenter.loadData() }.returns(Unit)

        // act
        presenter.onPermissionResult(true)

        // assert
        verify { presenter.loadData() }
    }

    @Test
    fun `onDoneButtonClicked should finish screen in pick directory mode`() {
        // arrange
        // act
        createPresenter(
            rootFile = ROOT_DIR,
            mode = Mode.PICK_DIRECTORY
        ).onDoneButtonClicked()

        // when
        verify { view.selectFileAndFinish(ROOT_DIR) }
    }

    @Test
    fun `onDoneButtonClicked should finish screen in pick file mode`() {
        // arrange
        setupFilesInMocks(ROOT_DIR_FILES)

        // act
        val presenter = createPresenter(
            rootFile = ROOT_DIR,
            mode = Mode.PICK_FILE,
            isBrowsingEnabled = true
        )
        presenter.loadData()
        presenter.onItemClicked(1)
        presenter.onDoneButtonClicked()

        // assert
        verify { view.selectFileAndFinish(ROOT_DIR_FILES[1]) }
    }

    @Test
    fun `onDoneButtonClicked should show show message if nothing selected in pick file mode`() {
        // arrange
        every { resources.getString(R.string.please_select_any_file) }.returns(MESSAGE)

        // act
        createPresenter(
            rootFile = ROOT_DIR,
            mode = Mode.PICK_FILE
        ).onDoneButtonClicked()

        // assert
        verify { resources.getString(R.string.please_select_any_file) }
        verify { view.showSnackbarMessage(MESSAGE) }
    }

    @Test
    fun `onItemClicked should mark view item selected`() {
        // arrange
        val file = createFile(path = "path")
        val item = VIEW_ITEM_MAPPER.map(file)
        setupFilesInMocks(listOf(file))

        // act
        val presenter = createPresenter(
            rootFile = ROOT_DIR,
            mode = Mode.PICK_FILE
        )
        presenter.loadData()
        presenter.onItemClicked(0)

        // assert
        val viewItem = item.copy(selected = true)
        verify { view.setItems(listOf(viewItem)) }
    }

    @Test
    fun `onItemClicked should change directory`() {
        // arrange
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(true)
        every { interactor.getFileList(ROOT_DIR) }.returns(OperationResult.success(ROOT_DIR_FILES))
        every { interactor.getFileList(CHILD_DIR) }.returns(OperationResult.success(CHILD_DIR_FILES))
        every { interactor.getParent(CHILD_DIR) }.returns(OperationResult.success(ROOT_DIR))
        every { dateFormatProvider.getShortDateFormat() }.returns(DATE_FORMAT)

        // act
        val presenter = createPresenter(
            rootFile = ROOT_DIR,
            isBrowsingEnabled = true
        )
        presenter.loadData()
        presenter.onItemClicked(0)

        // assert
        verify(exactly = 2) { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { interactor.getParent(CHILD_DIR) }
        verify { view.setDoneButtonVisibility(true) }
        verify { view.screenState = ScreenState.data() }
        verifyOrder {
            interactor.getFileList(ROOT_DIR)
            interactor.getFileList(CHILD_DIR)
        }
        val viewItems = mutableListOf<FilePickerAdapter.Item>()
        viewItems.add(VIEW_ITEM_MAPPER.map(CHILD_DIR).copy(title = ".."))
        viewItems.addAll(CHILD_DIR_VIEW_ITEMS)
        verifyOrder {
            view.setItems(ROOT_DIR_VIEW_ITEMS)
            view.setItems(viewItems)
        }
    }

    private fun createPresenter(
        view: FilePickerContract.View = this.view,
        mode: Mode = Mode.PICK_FILE,
        rootFile: FileDescriptor,
        isBrowsingEnabled: Boolean = true
    ): FilePickerPresenter {
        return FilePickerPresenter(view, mode, rootFile, isBrowsingEnabled)
    }

    private fun setupFilesInMocks(files: List<FileDescriptor>) {
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(true)
        every { interactor.getFileList(ROOT_DIR) }.returns(OperationResult.success(files))
        every { dateFormatProvider.getShortDateFormat() }.returns(DATE_FORMAT)
    }

    companion object {

        private const val EMPTY_TEXT = "Empty text"
        private const val ERROR_TEXT = "Error text"
        private const val MESSAGE = "Message"

        private const val PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        private val VIEW_ITEM_MAPPER = ViewItemMapper(DATE_FORMAT)

        private val ROOT_DIR = createFile(path = "root", isDirectory = true, isRoot = true)
        private val CHILD_DIR = createFile(path = "root/thirdDir", isDirectory = true)

        private val ROOT_DIR_FILES = listOf(
            CHILD_DIR,
            createFile(path = "root/firstChild"),
            createFile(path = "root/secondChild")
        )
        private val CHILD_DIR_FILES = listOf(
            createFile(path = CHILD_DIR.path + "/firstChild"),
            createFile(path = CHILD_DIR.path + "/secondChild")
        )

        private val ROOT_DIR_VIEW_ITEMS = VIEW_ITEM_MAPPER.map(ROOT_DIR_FILES)
        private val CHILD_DIR_VIEW_ITEMS = VIEW_ITEM_MAPPER.map(CHILD_DIR_FILES)

        private fun createFile(
            path: String,
            isDirectory: Boolean = false,
            isRoot: Boolean = false
        ): FileDescriptor {
            val file = FileDescriptor()

            file.fsType = FSType.REGULAR_FS
            file.modified = System.currentTimeMillis() // TODO: replace with constant
            file.isDirectory = isDirectory
            file.path = path
            file.uid = path
            file.isRoot = isRoot

            return file
        }
    }
}

