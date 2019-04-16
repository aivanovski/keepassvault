package com.ivanovsky.passnotes.presentation.newdb

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.presentation.core.FragmentState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class NewDatabasePresenterTest {

	companion object {
		private val PRIVATE_STORAGE = RuntimeEnvironment.application.getString(R.string.private_storage)
		private val PUBLIC_STORAGE = RuntimeEnvironment.application.getString(R.string.public_storage)
		private val DROPBOX = RuntimeEnvironment.application.getString(R.string.dropbox)

		private val ERROR_WAS_OCCURRED = RuntimeEnvironment.application.getString(R.string.error_was_occurred)
		private const val TEST_MESSAGE = "test_message"
	}

	@get:Rule
	val rule: TestRule = InstantTaskExecutorRule()

	@Test
	fun start_isSetScreenStateToData() {
		val presenter = NewDatabasePresenter(mock(Context::class.java))

		presenter.start()

		assertEquals(FragmentState.DISPLAYING_DATA, presenter.screenState.value?.state)
	}

	@Test
	fun onStorageSelected_shouldCreatePrivateStoragePath() {
		val mockedContext = mock(Context::class.java)

		Mockito.`when`(mockedContext.getString(R.string.private_storage)).thenReturn(PRIVATE_STORAGE)
		Mockito.`when`(mockedContext.filesDir).thenReturn(RuntimeEnvironment.application.filesDir)

		val presenter = NewDatabasePresenter(mockedContext)

		val file = File(RuntimeEnvironment.application.filesDir, "test.kdbx")
		val fileDescriptor = FileDescriptor.fromRegularFile(file)

		presenter.onStorageSelected(fileDescriptor)

		val storageTypeAndPath = presenter.storageTypeAndPath.value
		assertEquals(PRIVATE_STORAGE, storageTypeAndPath?.first)
		assertEquals(file.path, storageTypeAndPath?.second)
	}

	@Test
	fun onStorageSelected_shouldCreatePublicStoragePath() {
		val mockedContext = mock(Context::class.java)

		Mockito.`when`(mockedContext.getString(R.string.public_storage)).thenReturn(PUBLIC_STORAGE)
		Mockito.`when`(mockedContext.filesDir).thenReturn(RuntimeEnvironment.application.filesDir)

		val presenter = NewDatabasePresenter(mockedContext)

		val file = File("/sdcard/test.kdbx")
		val fileDescriptor = FileDescriptor.fromRegularFile(file)

		presenter.onStorageSelected(fileDescriptor)

		val storageTypeAndPath = presenter.storageTypeAndPath.value
		assertEquals(PUBLIC_STORAGE, storageTypeAndPath?.first)
		assertEquals(file.path, storageTypeAndPath?.second)
	}

	@Test
	fun onStorageSelected_shouldCreateDropboxStoragePath() {
		val mockedContext = mock(Context::class.java)
		val presenter = NewDatabasePresenter(mockedContext)

		Mockito.`when`(mockedContext.getString(R.string.dropbox)).thenReturn(DROPBOX)
		Mockito.`when`(mockedContext.filesDir).thenReturn(RuntimeEnvironment.application.filesDir)

		val file = FileDescriptor()

		file.fsType = FSType.DROPBOX
		file.path = "/test.kdbx"

		presenter.onStorageSelected(file)

		val storageTypeAndPath = presenter.storageTypeAndPath.value
		assertEquals(DROPBOX, storageTypeAndPath?.first)
		assertEquals(file.path, storageTypeAndPath?.second)
	}

	@Test
	fun selectStorage_shouldCreateShowStorageScreenAction() {
		val presenter = NewDatabasePresenter(mock(Context::class.java))
		val observer = mockedObserver<Void?>()
		presenter.showStorageScreenAction.observeForever(observer)

		presenter.selectStorage()

		Mockito.verify(observer).onChanged(null)
	}

//	@Test
//	fun onCreateDatabaseResult_databaseSuccessfullyCreated_shouldCreateShowGroupsScreenAction() {
//		val result = OperationResult.success(true)
//		val presenter = NewDatabasePresenter(mock(Context::class.java))
//		val observer = mockedObserver<Void?>()
//		presenter.showGroupsScreenAction.observeForever(observer)
//
//		presenter.onCreateDatabaseResult(result)
//
//		Mockito.verify(observer).onChanged(null)
//	}
//
//	@Test
//	fun onCreateDatabaseResult_databaseIsNotCreated_shouldShowErrorWasOccurred() {
//		val result = OperationResult.success(false)
//		val mockedContext = mock(Context::class.java)
//		val presenter = NewDatabasePresenter(mockedContext)
//
//		Mockito.`when`(mockedContext.getString(R.string.error_was_occurred)).thenReturn(ERROR_WAS_OCCURRED)
//
//		presenter.onCreateDatabaseResult(result)
//
//		val screenState = presenter.screenState.value
//		assertEquals(FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL, screenState?.state)
//		assertEquals(ERROR_WAS_OCCURRED, screenState?.message)
//		assertEquals(true, presenter.doneButtonVisibility.value)
//	}
//
//	@Test
//	fun onCreateDatabaseResult_databaseCreationFailed_shouldShowErrorWithMessage() {
//		val result = OperationResult.error<Boolean>(OperationError.newGenericError(TEST_MESSAGE))
//		val presenter = NewDatabasePresenter(mock(Context::class.java))
//		val mockedErrorInteractor = mock(ErrorInteractor::class.java)
//
//		Mockito.`when`(mockedErrorInteractor.processAndGetMessage(any())).thenReturn(TEST_MESSAGE)
//
//		presenter.onCreateDatabaseResult(result)
//
//		val screenState = presenter.screenState.value
//		assertEquals(FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL, screenState?.state)
//		assertEquals(TEST_MESSAGE, screenState?.message)
//		assertEquals(true, presenter.doneButtonVisibility.value)
//	}

	@Suppress("UNCHECKED_CAST")
	private fun <T> mockedObserver(): Observer<T> {
		return mock(Observer::class.java) as Observer<T>
	}
}