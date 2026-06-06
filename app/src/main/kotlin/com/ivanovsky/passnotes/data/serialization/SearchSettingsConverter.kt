package com.ivanovsky.passnotes.data.serialization

import com.ivanovsky.passnotes.domain.entity.SearchOptions
import com.ivanovsky.passnotes.domain.entity.SearchScope
import com.ivanovsky.passnotes.domain.entity.SearchType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object SearchSettingsConverter {

    const val KEY_SEARCH_TYPE = "searchType"
    const val KEY_IS_TITLE_ENABLED = "isTitleEnabled"
    const val KEY_IS_USERNAME_ENABLED = "isUsernameEnabled"
    const val KEY_IS_PASSWORD_ENABLED = "isPasswordEnabled"
    const val KEY_IS_URL_ENABLED = "isUrlEnabled"
    const val KEY_IS_NOTES_ENABLED = "isNotesEnabled"
    const val KEY_IS_OTHER_FIELDS_ENABLED = "isOtherFieldsEnabled"
    const val KEY_RESTRICTION_SCOPES = "restrictionScopes"
    const val KEY_IS_CASE_SENSITIVE = "isCaseSensitive"

    fun toString(settings: SearchOptions): String? {
        return try {
            val obj = JSONObject()

            obj.put(KEY_SEARCH_TYPE, settings.searchType.name)
            obj.put(KEY_IS_TITLE_ENABLED, settings.isTitleEnabled)
            obj.put(KEY_IS_USERNAME_ENABLED, settings.isUsernameEnabled)
            obj.put(KEY_IS_PASSWORD_ENABLED, settings.isPasswordEnabled)
            obj.put(KEY_IS_URL_ENABLED, settings.isUrlEnabled)
            obj.put(KEY_IS_NOTES_ENABLED, settings.isNotesEnabled)
            obj.put(KEY_IS_OTHER_FIELDS_ENABLED, settings.isOtherFieldsEnabled)
            obj.put(
                KEY_RESTRICTION_SCOPES,
                JSONArray(settings.restrictionScopes.map { it.name })
            )
            obj.put(KEY_IS_CASE_SENSITIVE, settings.isCaseSensitive)

            obj.toString()
        } catch (e: JSONException) {
            Timber.d(e)
            null
        }
    }

    fun fromString(data: String): SearchOptions? {
        return try {
            val obj = JSONObject(data)
            val defaultSettings = SearchOptions.DEFAULT

            val searchType = obj.optString(KEY_SEARCH_TYPE)
                .takeIf { it.isNotEmpty() }
                ?.let { SearchType.getByName(it) }
                ?: defaultSettings.searchType

            val isTitleEnabled = obj.optBoolean(
                KEY_IS_TITLE_ENABLED,
                defaultSettings.isTitleEnabled
            )
            val isUsernameEnabled = obj.optBoolean(
                KEY_IS_USERNAME_ENABLED,
                defaultSettings.isUsernameEnabled
            )
            val isPasswordEnabled = obj.optBoolean(
                KEY_IS_PASSWORD_ENABLED,
                defaultSettings.isPasswordEnabled
            )
            val isUrlEnabled = obj.optBoolean(
                KEY_IS_URL_ENABLED,
                defaultSettings.isUrlEnabled
            )
            val isNotesEnabled = obj.optBoolean(
                KEY_IS_NOTES_ENABLED,
                defaultSettings.isNotesEnabled
            )
            val isOtherFieldsEnabled = obj.optBoolean(
                KEY_IS_OTHER_FIELDS_ENABLED,
                defaultSettings.isOtherFieldsEnabled
            )
            val isCaseSensitive = obj.optBoolean(
                KEY_IS_CASE_SENSITIVE,
                defaultSettings.isCaseSensitive
            )
            val scopes = obj.optJSONArray(KEY_RESTRICTION_SCOPES)
                ?.toSearchScopes()
                ?: defaultSettings.restrictionScopes

            SearchOptions(
                searchType = searchType,
                isTitleEnabled = isTitleEnabled,
                isUsernameEnabled = isUsernameEnabled,
                isPasswordEnabled = isPasswordEnabled,
                isUrlEnabled = isUrlEnabled,
                isNotesEnabled = isNotesEnabled,
                isOtherFieldsEnabled = isOtherFieldsEnabled,
                restrictionScopes = scopes,
                isCaseSensitive = isCaseSensitive
            )
        } catch (e: JSONException) {
            Timber.d(e)
            null
        }
    }

    private fun JSONArray.toSearchScopes(): Set<SearchScope> {
        return (0 until length())
            .mapNotNull { idx ->
                optString(idx)
                    .takeIf { it.isNotEmpty() }
                    ?.let { name -> SearchScope.entries.firstOrNull { it.name == name } }
            }
            .toSet()
    }
}