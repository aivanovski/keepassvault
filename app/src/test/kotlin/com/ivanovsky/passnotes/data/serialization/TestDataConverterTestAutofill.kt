package com.ivanovsky.passnotes.data.serialization

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.data.entity.TestAutofillData
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_FAKE_FS_PASSWORD
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_FAKE_FS_URL
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_FAKE_FS_USERNAME
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_FILENAME_PATTERNS
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_GIT_URL
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_PASSWORDS
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_WEBDAV_PASSWORD
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_WEBDAV_URL
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter.KEY_WEBDAV_USERNAME
import com.ivanovsky.passnotes.extensions.optStringArray
import com.ivanovsky.passnotes.util.toJSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class TestDataConverterTestAutofill {

    @Test
    fun `toString should convert data to json string`() {
        // arrange
        val data = newTestData()

        // act
        val json = TestAutofillDataConverter.toString(data)

        // assert
        requireNotNull(json)

        val obj = JSONObject(json)

        assertThat(obj.optStringArray(KEY_FILENAME_PATTERNS).toList()).isEqualTo(FILENAME_PATTERNS)
        assertThat(obj.optStringArray(KEY_PASSWORDS).toList()).isEqualTo(PASSWORDS)
        assertThat(obj.optString(KEY_WEBDAV_URL)).isEqualTo(WEBDAV_URL)
        assertThat(obj.optString(KEY_WEBDAV_USERNAME)).isEqualTo(USERNAME)
        assertThat(obj.optString(KEY_WEBDAV_PASSWORD)).isEqualTo(PASSWORD)
        assertThat(obj.optString(KEY_GIT_URL)).isEqualTo(GIT_URL)
        assertThat(obj.optString(KEY_FAKE_FS_URL)).isEqualTo(FAKE_FS_URL)
        assertThat(obj.optString(KEY_FAKE_FS_USERNAME)).isEqualTo(FAKE_FS_USERNAME)
        assertThat(obj.optString(KEY_FAKE_FS_PASSWORD)).isEqualTo(FAKE_FS_PASSWORD)
    }

    @Test
    fun `fromString should parse json string`() {
        // arrange
        val expectedData = newTestData()
        val json = JSONObject()
            .apply {
                put(KEY_FILENAME_PATTERNS, expectedData.filenamePatterns.toJSONArray())
                put(KEY_PASSWORDS, expectedData.passwords.toJSONArray())
                put(KEY_WEBDAV_URL, expectedData.webdavUrl)
                put(KEY_WEBDAV_USERNAME, expectedData.webdavUsername)
                put(KEY_WEBDAV_PASSWORD, expectedData.webdavPassword)
                put(KEY_GIT_URL, expectedData.gitUrl)
                put(KEY_FAKE_FS_URL, expectedData.fakeFsUrl)
                put(KEY_FAKE_FS_USERNAME, expectedData.fakeFsUsername)
                put(KEY_FAKE_FS_PASSWORD, expectedData.fakeFsPassword)
            }
            .toString()

        // act
        val data = TestAutofillDataConverter.fromString(json)

        // assert
        assertThat(data).isEqualTo(expectedData)
    }

    @Test
    fun `fromString should return null`() {
        val data = TestAutofillDataConverter.fromString("")
        assertThat(data).isNull()
    }

    private fun newTestData(): TestAutofillData =
        TestAutofillData(
            filenamePatterns = FILENAME_PATTERNS,
            passwords = PASSWORDS,
            webdavUrl = WEBDAV_URL,
            webdavUsername = USERNAME,
            webdavPassword = PASSWORD,
            gitUrl = GIT_URL,
            fakeFsUrl = FAKE_FS_URL,
            fakeFsUsername = FAKE_FS_USERNAME,
            fakeFsPassword = FAKE_FS_PASSWORD
        )

    companion object {
        private const val WEBDAV_URL = "https://webdav-url.com"
        private const val GIT_URL = "https://git-url.com"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val FAKE_FS_URL = "fake_url"
        private const val FAKE_FS_USERNAME = "fake_username"
        private const val FAKE_FS_PASSWORD = "fake_password"
        private val FILENAME_PATTERNS = listOf("pattern1", "pattern2")
        private val PASSWORDS = listOf("password1", "password2")
    }
}