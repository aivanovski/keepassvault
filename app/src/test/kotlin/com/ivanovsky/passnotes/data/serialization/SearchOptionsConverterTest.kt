package com.ivanovsky.passnotes.data.serialization

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_CASE_SENSITIVE
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_NOTES_ENABLED
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_OTHER_FIELDS_ENABLED
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_PASSWORD_ENABLED
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_TITLE_ENABLED
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_URL_ENABLED
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_IS_USERNAME_ENABLED
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_RESTRICTION_SCOPES
import com.ivanovsky.passnotes.data.serialization.SearchSettingsConverter.KEY_SEARCH_TYPE
import com.ivanovsky.passnotes.domain.entity.SearchOptions
import com.ivanovsky.passnotes.domain.entity.SearchScope
import com.ivanovsky.passnotes.domain.entity.SearchType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class SearchOptionsConverterTest {

    @Test
    fun `toString should convert data to json string`() {
        // arrange
        val data = newOptions()

        // act
        val json = SearchSettingsConverter.toString(data)

        // assert
        requireNotNull(json)

        val obj = JSONObject(json)
        val scopes = obj.optJSONArray(KEY_RESTRICTION_SCOPES) ?: JSONArray()

        val result = SearchOptions(
            searchType = SearchType.getByName(obj.optString(KEY_SEARCH_TYPE))
                ?: SearchType.default(),
            isTitleEnabled = obj.optBoolean(KEY_IS_TITLE_ENABLED),
            isUsernameEnabled = obj.optBoolean(KEY_IS_USERNAME_ENABLED),
            isPasswordEnabled = obj.optBoolean(KEY_IS_PASSWORD_ENABLED),
            isUrlEnabled = obj.optBoolean(KEY_IS_URL_ENABLED),
            isNotesEnabled = obj.optBoolean(KEY_IS_NOTES_ENABLED),
            isOtherFieldsEnabled = obj.optBoolean(KEY_IS_OTHER_FIELDS_ENABLED),
            restrictionScopes = (0 until scopes.length())
                .mapNotNull { idx ->
                    SearchScope.entries.firstOrNull { it.name == scopes.optString(idx) }
                }
                .toSet(),
            isCaseSensitive = obj.optBoolean(KEY_IS_CASE_SENSITIVE)
        )

        assertThat(result).isEqualTo(data)
    }

    @Test
    fun `fromString should parse json string`() {
        // arrange
        val expected = newOptions()
        val json = JSONObject()
            .apply {
                put(KEY_SEARCH_TYPE, expected.searchType.name)
                put(KEY_IS_TITLE_ENABLED, expected.isTitleEnabled)
                put(KEY_IS_USERNAME_ENABLED, expected.isUsernameEnabled)
                put(KEY_IS_PASSWORD_ENABLED, expected.isPasswordEnabled)
                put(KEY_IS_URL_ENABLED, expected.isUrlEnabled)
                put(KEY_IS_NOTES_ENABLED, expected.isNotesEnabled)
                put(KEY_IS_OTHER_FIELDS_ENABLED, expected.isOtherFieldsEnabled)
                put(
                    KEY_RESTRICTION_SCOPES,
                    JSONArray(expected.restrictionScopes.map { it.name })
                )
                put(KEY_IS_CASE_SENSITIVE, expected.isCaseSensitive)
            }
            .toString()

        // act
        val result = SearchSettingsConverter.fromString(json)

        // assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `fromString should use defaults for missing values`() {
        // arrange
        val json = JSONObject()
            .apply {
                put(KEY_SEARCH_TYPE, SearchType.FUZZY.name)
            }
            .toString()

        // act
        val result = SearchSettingsConverter.fromString(json)

        // assert
        assertThat(result).isEqualTo(SearchOptions.DEFAULT.copy(searchType = SearchType.FUZZY))
    }

    @Test
    fun `fromString should return null`() {
        val result = SearchSettingsConverter.fromString("invalid json")
        assertThat(result).isNull()
    }

    private fun newOptions(): SearchOptions =
        SearchOptions(
            searchType = SearchType.FUZZY,
            isTitleEnabled = false,
            isUsernameEnabled = true,
            isPasswordEnabled = true,
            isUrlEnabled = false,
            isNotesEnabled = true,
            isOtherFieldsEnabled = false,
            restrictionScopes = setOf(SearchScope.SEARCHABLE, SearchScope.RECYCLE_BIN),
            isCaseSensitive = true
        )
}