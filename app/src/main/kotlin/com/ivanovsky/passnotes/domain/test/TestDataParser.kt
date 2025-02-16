package com.ivanovsky.passnotes.domain.test

import android.os.Bundle
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.TestAutofillData
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.domain.test.entity.TestArguments
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toBooleanSafely

class TestDataParser {

    fun parse(data: Bundle): OperationResult<TestArguments> {
        val isResetAppData = data.getString(IS_RESET_APP_DATA)?.toBooleanSafely() ?: false
        val isResetTestData = data.getString(IS_RESET_TEST_DATA)?.toBooleanSafely() ?: false
        val fakeFileName = data.getString(FAKE_FILE_NAME)
        val autofillData = parseTestData(data)

        if ((isResetTestData && !fakeFileName.isNullOrEmpty()) ||
            (isResetTestData && autofillData != null) ||
            (isResetAppData && !fakeFileName.isNullOrEmpty()) ||
            (isResetAppData && autofillData != null) ||
            (!fakeFileName.isNullOrEmpty() && autofillData != null)
        ) {
            return OperationResult.error(
                newGenericError(
                    ERROR_INVALID_ARGUMENTS_SPECIFIED,
                    Stacktrace()
                )
            )
        }

        return if (isResetAppData ||
            isResetTestData ||
            fakeFileName != null ||
            autofillData != null
        ) {
            OperationResult.success(
                TestArguments(
                    isResetAppData = isResetAppData,
                    isResetTestData = isResetTestData,
                    testFakeFileName = fakeFileName,
                    testAutofillData = autofillData
                )
            )
        } else {
            OperationResult.error(newGenericError(ERROR_NO_ARGUMENTS_SPECIFIED, Stacktrace()))
        }
    }

    private fun parseTestData(data: Bundle): TestAutofillData? {
        val filenamePatterns = data.getStringArray(FILENAME_PATTERNS)
            ?.toList()
            ?: emptyList()

        val passwords = data.getStringArray(PASSWORDS)
            ?.toList()
            ?: emptyList()

        val webdavUrl = data.getString(WEBDAV_URL, EMPTY)
        val webdavUsername = data.getString(WEBDAV_USERNAME, EMPTY)
        val webdavPassword = data.getString(WEBDAV_PASSWORD, EMPTY)
        val gitUrl = data.getString(GIT_URL, EMPTY)
        val fakeFsUrl = data.getString(FAKE_FS_URL, EMPTY)
        val fakeFsUsername = data.getString(FAKE_FS_USERNAME, EMPTY)
        val fakeFsPassword = data.getString(FAKE_FS_PASSWORD, EMPTY)

        return if (webdavUrl.isNotEmpty() ||
            webdavUsername.isNotEmpty() ||
            webdavPassword.isNotEmpty() ||
            gitUrl.isNotEmpty() ||
            (filenamePatterns.isNotEmpty() && passwords.isNotEmpty()) ||
            fakeFsUrl.isNotEmpty() ||
            fakeFsUsername.isNotEmpty() ||
            fakeFsPassword.isNotEmpty()
        ) {
            TestAutofillData(
                filenamePatterns = filenamePatterns,
                passwords = passwords,
                webdavUrl = webdavUrl,
                webdavUsername = webdavUsername,
                webdavPassword = webdavPassword,
                gitUrl = gitUrl,
                fakeFsUrl = fakeFsUrl,
                fakeFsUsername = fakeFsUsername,
                fakeFsPassword = fakeFsPassword
            )
        } else {
            null
        }
    }

    companion object {
        const val IS_RESET_TEST_DATA = "isResetStoredTestData"
        const val IS_RESET_APP_DATA = "isResetAppData"
        const val FAKE_FILE_NAME = "fakeFileName"

        const val FILENAME_PATTERNS = "filenamePatterns"
        const val PASSWORDS = "passwords"
        const val WEBDAV_URL = "webdavUrl"
        const val WEBDAV_USERNAME = "webdavUsername"
        const val WEBDAV_PASSWORD = "webdavPassword"
        const val GIT_URL = "gitUrl"
        const val FAKE_FS_URL = "fakeFsUrl"
        const val FAKE_FS_USERNAME = "fakeFsUsername"
        const val FAKE_FS_PASSWORD = "fakeFsPassword"

        const val ERROR_INVALID_ARGUMENTS_SPECIFIED = "Invalid arguments specified"
        const val ERROR_NO_ARGUMENTS_SPECIFIED = "No arguments specified"
    }
}