package com.ivanovsky.passnotes.domain.test

import android.os.Bundle
import com.ivanovsky.passnotes.data.entity.TestData
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class TestDataExtractor {

    fun extractFromBundle(data: Bundle): TestData? {
        val filenamePatterns = data.getStringArray(KEY_FILENAME_PATTERNS)
            ?.toList()
            ?: emptyList()

        val passwords = data.getStringArray(KEY_PASSWORDS)
            ?.toList()
            ?: emptyList()

        val webdavUrl = data.getString(KEY_WEBDAV_URL, EMPTY)
        val webdavUsername = data.getString(KEY_WEBDAV_USERNAME, EMPTY)
        val webdavPassword = data.getString(KEY_WEBDAV_PASSWORD, EMPTY)
        val gitUrl = data.getString(KEY_GIT_URL, EMPTY)
        val fakeFsUrl = data.getString(KEY_FAKE_FS_URL, EMPTY)
        val fakeFsUsername = data.getString(KEY_FAKE_FS_USERNAME, EMPTY)
        val fakeFsPassword = data.getString(KEY_FAKE_FS_PASSWORD, EMPTY)

        return if (webdavUrl.isNotEmpty() ||
            webdavUsername.isNotEmpty() ||
            webdavPassword.isNotEmpty() ||
            gitUrl.isNotEmpty() ||
            (filenamePatterns.isNotEmpty() && passwords.isNotEmpty())
        ) {
            TestData(
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
        private const val KEY_FILENAME_PATTERNS = "filenamePatterns"
        private const val KEY_PASSWORDS = "passwords"
        private const val KEY_WEBDAV_URL = "webdavUrl"
        private const val KEY_WEBDAV_USERNAME = "webdavUsername"
        private const val KEY_WEBDAV_PASSWORD = "webdavPassword"
        private const val KEY_GIT_URL = "gitUrl"
        private const val KEY_FAKE_FS_URL = "fakeFsUrl"
        private const val KEY_FAKE_FS_USERNAME = "fakeFsUsername"
        private const val KEY_FAKE_FS_PASSWORD = "fakeFsPassword"
    }
}