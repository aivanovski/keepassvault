package com.ivanovsky.passnotes.domain.test

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.TestAutofillData
import com.ivanovsky.passnotes.domain.test.TestCommandParser.Companion.ERROR_INVALID_ARGUMENTS_SPECIFIED
import com.ivanovsky.passnotes.domain.test.TestCommandParser.Companion.ERROR_NO_ARGUMENTS_SPECIFIED
import com.ivanovsky.passnotes.domain.test.entity.TestCommand
import com.ivanovsky.passnotes.extensions.getOrThrow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class TestCommandParserTest {

    @Test
    fun `parse should return isResetTestData flag`() {
        // arrange
        val data = Bundle()
            .apply {
                putBoolean(TestCommandParser.IS_RESET_TEST_DATA, true)
            }

        // act
        val result = newParser().parse(data)

        // assert
        assertThat(result.getOrThrow()).isEqualTo(TestCommand.ResetTestData)
    }

    @Test
    fun `parse should return fakeFileName`() {
        // arrange
        val data = Bundle()
            .apply {
                putString(TestCommandParser.FAKE_FILE_NAME, FILE_NAME)
            }

        // act
        val result = newParser().parse(data)

        // assert
        assertThat(result.getOrThrow()).isEqualTo(TestCommand.SetupFakeFile(FILE_NAME))
    }

    @Test
    fun `parse should return TestAutofillData`() {
        // arrange
        val data = Bundle()
            .apply {
                putStringArray(TestCommandParser.FILENAME_PATTERNS, PATTERNS.toTypedArray())
                putStringArray(TestCommandParser.PASSWORDS, PASSWORDS.toTypedArray())
                putString(TestCommandParser.WEBDAV_URL, WEBDAV_URL)
                putString(TestCommandParser.WEBDAV_USERNAME, WEBDAV_USERNAME)
                putString(TestCommandParser.WEBDAV_PASSWORD, WEBDAV_PASSWORD)
                putString(TestCommandParser.GIT_URL, GIT_URL)
                putString(TestCommandParser.FAKE_FS_URL, FAKE_FS_URL)
                putString(TestCommandParser.FAKE_FS_USERNAME, FAKE_FS_USERNAME)
                putString(TestCommandParser.FAKE_FS_PASSWORD, FAKE_FS_PASSWORD)
            }

        // act
        val result = newParser().parse(data)

        // assert
        assertThat(result.getOrThrow()).isEqualTo(
            TestCommand.SetupAutofillData(
                autofillData = TestAutofillData(
                    filenamePatterns = PATTERNS,
                    passwords = PASSWORDS,
                    webdavUrl = WEBDAV_URL,
                    webdavUsername = WEBDAV_USERNAME,
                    webdavPassword = WEBDAV_PASSWORD,
                    gitUrl = GIT_URL,
                    fakeFsUrl = FAKE_FS_URL,
                    fakeFsUsername = FAKE_FS_USERNAME,
                    fakeFsPassword = FAKE_FS_PASSWORD
                )
            )
        )
    }

    @Test
    fun `parse should return when two commands specified`() {
        listOf(
            Bundle().apply {
                putBoolean(TestCommandParser.IS_RESET_TEST_DATA, true)
                putString(TestCommandParser.FAKE_FILE_NAME, FILE_NAME)
            },

            Bundle().apply {
                putBoolean(TestCommandParser.IS_RESET_TEST_DATA, true)
                putString(TestCommandParser.WEBDAV_URL, WEBDAV_URL)
            },

            Bundle().apply {
                putString(TestCommandParser.FAKE_FILE_NAME, FILE_NAME)
                putString(TestCommandParser.WEBDAV_URL, WEBDAV_URL)
            }
        )
            .forEach { data ->
                // act
                val result = newParser().parse(data)

                // assert
                assertThat(result.error).isEqualTo(
                    OperationError.newGenericError(
                        ERROR_INVALID_ARGUMENTS_SPECIFIED
                    )
                )
            }
    }

    @Test
    fun `parse should return error if nothing specified`() {
        // act
        val result = newParser().parse(Bundle())

        // assert
        assertThat(result.error).isEqualTo(
            OperationError.newGenericError(
                ERROR_NO_ARGUMENTS_SPECIFIED
            )
        )
    }

    private fun newParser(): TestCommandParser =
        TestCommandParser()

    companion object {
        private const val FILE_NAME = "file.kdbx"
        private val PATTERNS = listOf("pattern1", "pattern2")
        private val PASSWORDS = listOf("password1", "password2")
        private const val WEBDAV_URL = "https://webdav.com"
        private const val WEBDAV_USERNAME = "webdavUsername"
        private const val WEBDAV_PASSWORD = "webdavPassword"
        private const val GIT_URL = "https://git.com"
        private const val FAKE_FS_URL = "https://fakefs.com"
        private const val FAKE_FS_USERNAME = "fakeFsUsername"
        private const val FAKE_FS_PASSWORD = "fakeFsPassword"
    }
}