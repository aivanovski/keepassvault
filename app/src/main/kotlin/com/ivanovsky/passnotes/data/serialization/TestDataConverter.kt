package com.ivanovsky.passnotes.data.serialization

import com.ivanovsky.passnotes.data.entity.TestData
import com.ivanovsky.passnotes.extensions.optStringArray
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object TestDataConverter {

    const val KEY_FILENAME_PATTERNS = "filenamePatterns"
    const val KEY_PASSWORDS = "passwords"
    const val KEY_WEBDAV_URL = "webdavUrl"
    const val KEY_WEBDAV_USERNAME = "webdavUsername"
    const val KEY_WEBDAV_PASSWORD = "webdavPassword"
    const val KEY_GIT_URL = "gitUrl"
    const val KEY_FAKE_FS_URL = "fakeFsUrl"
    const val KEY_FAKE_FS_USERNAME = "fakeFsUsername"
    const val KEY_FAKE_FS_PASSWORD = "fakeFsPassword"

    fun toString(data: TestData): String? {
        return try {
            val obj = JSONObject()

            val filenamePatternArray = JSONArray()
                .apply {
                    for (pattern in data.filenamePatterns) {
                        put(pattern)
                    }
                }

            val passwordArray = JSONArray()
                .apply {
                    for (password in data.passwords) {
                        put(password)
                    }
                }

            obj.put(KEY_FILENAME_PATTERNS, filenamePatternArray)
            obj.put(KEY_PASSWORDS, passwordArray)
            obj.put(KEY_WEBDAV_URL, data.webdavUrl)
            obj.put(KEY_WEBDAV_USERNAME, data.webdavUsername)
            obj.put(KEY_WEBDAV_PASSWORD, data.webdavPassword)
            obj.put(KEY_GIT_URL, data.gitUrl)
            obj.put(KEY_FAKE_FS_URL, data.fakeFsUrl)
            obj.put(KEY_FAKE_FS_USERNAME, data.fakeFsUsername)
            obj.put(KEY_FAKE_FS_PASSWORD, data.fakeFsPassword)

            obj.toString()
        } catch (e: JSONException) {
            Timber.d(e)
            null
        }
    }

    fun fromString(data: String): TestData? {
        return try {
            val obj = JSONObject(data)

            val filenamePatterns = obj.optStringArray(KEY_FILENAME_PATTERNS).toList()
            val passwords = obj.optStringArray(KEY_PASSWORDS).toList()
            val webdavUrl = obj.optString(KEY_WEBDAV_URL)
            val webdavUsername = obj.optString(KEY_WEBDAV_USERNAME)
            val webdavPassword = obj.optString(KEY_WEBDAV_PASSWORD)
            val gitUrl = obj.optString(KEY_GIT_URL)
            val fakeFsUrl = obj.optString(KEY_FAKE_FS_URL)
            val fakeFsUsername = obj.optString(KEY_FAKE_FS_USERNAME)
            val fakeFsPassword = obj.optString(KEY_FAKE_FS_PASSWORD)

            if (webdavUrl.isNotEmpty() ||
                webdavUsername.isNotEmpty() ||
                webdavPassword.isNotEmpty() ||
                gitUrl.isNotEmpty() ||
                (filenamePatterns.isNotEmpty() && passwords.isNotEmpty()) ||
                fakeFsUrl.isNotEmpty() ||
                fakeFsUsername.isNotEmpty() ||
                fakeFsPassword.isNotEmpty()
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
        } catch (e: JSONException) {
            null
        }
    }
}