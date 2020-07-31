package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.TEST_DISPATCHER_PROVIDER
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.util.formatAccordingLocale
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.text.SimpleDateFormat
import java.util.*

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
        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(false)

        createPresenter(
            view = view,
            rootFile = ROOT_DIR
        ).loadData()

        verify { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { view.requestPermission(PERMISSION) }
        verify { view.setDoneButtonVisibility(false) }
        verify { view.screenState = ScreenState.loading() }
    }

    @Test
    fun `loadData should load files`() {
        val adapterItems = FILE_LIST.map { file ->
            FilePickerAdapter.Item(
                R.drawable.ic_file_white_24dp,
                file.name,
                DATE_FORMAT.format(Date(file.modified)),
                false
            )
        }.toMutableList()

        every { permissionHelper.isPermissionGranted(PERMISSION) }.returns(true)
        every { interactor.getFileList(ROOT_DIR) }.returns(OperationResult.success(FILE_LIST))
        every { dateFormatProvider.getShortDateFormat() }.returns(DATE_FORMAT)

        createPresenter(
            view = view,
            rootFile = ROOT_DIR,
            isBrowsingEnabled = true
        ).loadData()

        verify { permissionHelper.isPermissionGranted(PERMISSION) }
        verify { view.setDoneButtonVisibility(true) }
        verify { view.screenState = ScreenState.data() }
        verify { view.setItems(adapterItems) }
    }

    @Test
    fun `onDoneButtonClicked should finish screen for directory mode`() {
        createPresenter(
            view = view,
            mode = Mode.PICK_DIRECTORY,
            rootFile = ROOT_DIR
        ).onDoneButtonClicked()

        verify { view.selectFileAndFinish(ROOT_DIR) }
    }

    private fun createPresenter(
        view: FilePickerContract.View = this.view,
        mode: Mode = Mode.PICK_FILE,
        rootFile: FileDescriptor,
        isBrowsingEnabled: Boolean = true
    ): FilePickerPresenter {
        return FilePickerPresenter(view, mode, rootFile, isBrowsingEnabled)
    }

    companion object {

        private const val PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        private val ROOT_DIR = createFile(path = "root", isDirectory = true, isRoot = true)

        private val FILE_LIST = listOf(
            createFile(path = "root/firstChild"),
            createFile(path = "root/secondChild"),
            createFile(path = "root/thirdChild")
        )

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

