package com.ivanovsky.passnotes.data.serialization

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.data.entity.TestData
import com.ivanovsky.passnotes.data.serialization.TestDataConverter.KEY_FILENAME_PATTERNS
import com.ivanovsky.passnotes.data.serialization.TestDataConverter.KEY_GIT_URL
import com.ivanovsky.passnotes.data.serialization.TestDataConverter.KEY_PASSWORDS
import com.ivanovsky.passnotes.data.serialization.TestDataConverter.KEY_WEBDAV_PASSWORD
import com.ivanovsky.passnotes.data.serialization.TestDataConverter.KEY_WEBDAV_URL
import com.ivanovsky.passnotes.data.serialization.TestDataConverter.KEY_WEBDAV_USERNAME
import com.ivanovsky.passnotes.extensions.optStringArray
import com.ivanovsky.passnotes.util.toJSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class TestDataConverterTest {

    @Test
    fun `toString should convert data to json string`() {
        // arrange
        val data = newTestData()

        // act
        val json = TestDataConverter.toString(data)

        // assert
        requireNotNull(json)

        val obj = JSONObject(json)

        assertThat(obj.optStringArray(KEY_FILENAME_PATTERNS).toList()).isEqualTo(FILENAME_PATTERNS)
        assertThat(obj.optStringArray(KEY_PASSWORDS).toList()).isEqualTo(PASSWORDS)
        assertThat(obj.optString(KEY_WEBDAV_URL)).isEqualTo(WEBDAV_URL)
        assertThat(obj.optString(KEY_WEBDAV_USERNAME)).isEqualTo(USERNAME)
        assertThat(obj.optString(KEY_WEBDAV_PASSWORD)).isEqualTo(PASSWORD)
        assertThat(obj.optString(KEY_GIT_URL)).isEqualTo(GIT_URL)
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
            }
            .toString()

        // act
        val data = TestDataConverter.fromString(json)

        // assert
        assertThat(data).isEqualTo(expectedData)
    }

    @Test
    fun `fromString should return null`() {
        val data = TestDataConverter.fromString("")
        assertThat(data).isNull()
    }

    private fun newTestData(): TestData =
        TestData(
            filenamePatterns = FILENAME_PATTERNS,
            passwords = PASSWORDS,
            webdavUrl = WEBDAV_URL,
            webdavUsername = USERNAME,
            webdavPassword = PASSWORD,
            gitUrl = GIT_URL
        )

    companion object {
        private const val WEBDAV_URL = "https://webdav-url.com"
        private const val GIT_URL = "https://git-url.com"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private val FILENAME_PATTERNS = listOf("pattern1", "pattern2")
        private val PASSWORDS = listOf("password1", "password2")
    }
}