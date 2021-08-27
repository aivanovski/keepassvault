package com.ivanovsky.passnotes.data.repository.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.PrefType.BOOLEAN
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.PrefType.INT
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.PrefType.STRING
import com.ivanovsky.passnotes.domain.ResourceProvider
import java.util.concurrent.TimeUnit

class SettingsImpl(
    private val resourceProvider: ResourceProvider,
    context: Context
) : Settings {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val nameResIdToPreferenceMap: Map<Int, Pref> = PREFS.associateBy { it.keyId }

    override var isExternalStorageCacheEnabled: Boolean
        get() = getBoolean(R.string.pref_is_external_storage_cache_enabled)
        set(value) {
            putBoolean(R.string.pref_is_external_storage_cache_enabled, value)
        }

    override var isLockNotificationVisible: Boolean
        get() = getBoolean(R.string.pref_is_lock_notification_visible)
        set(value) {
            putBoolean(R.string.pref_is_lock_notification_visible, value)
        }

    override var autoLockDelayInMs: Int
        get() = getInt(R.string.pref_auto_lock_delay_in_ms)
        set(value) {
            putInt(R.string.pref_auto_lock_delay_in_ms, value)
        }

    override var dropboxAuthToken: String?
        get() = getString(R.string.pref_dropbox_auth_token)
        set(value) {
            putString(R.string.pref_dropbox_auth_token, value)
        }

    fun clean() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun getBoolean(@StringRes keyId: Int): Boolean {
        return preferences.getBoolean(keyFor(keyId), getDefaultValue(keyId))
    }

    private fun getString(@StringRes keyId: Int): String? {
        return preferences.getString(keyFor(keyId), getDefaultValue(keyId))
    }

    private fun getInt(@StringRes keyId: Int): Int {
        return preferences.getInt(keyFor(keyId), getDefaultValue(keyId))
    }

    private fun putBoolean(@StringRes keyId: Int, value: Boolean) {
        putValue {
            putBoolean(keyFor(keyId), value)
        }
    }

    private fun putString(@StringRes keyId: Int, value: String?) {
        putValue {
            putString(keyFor(keyId), value)
        }
    }

    private fun putInt(@StringRes keyId: Int, value: Int) {
        putValue {
            putInt(keyFor(keyId), value)
        }
    }

    private inline fun putValue(action: SharedPreferences.Editor.() -> Unit) {
        val editor = preferences.edit()
        action.invoke(editor)
        editor.apply()
    }

    private fun keyFor(@StringRes keyId: Int) = resourceProvider.getString(keyId)

    private inline fun <reified T> getDefaultValue(@StringRes keyId: Int): T {
        return nameResIdToPreferenceMap[keyId]?.defaultValue as T
    }

    enum class PrefType {
        BOOLEAN,
        INT,
        STRING
    }

    data class Pref(
        @StringRes val keyId: Int,
        val type: PrefType,
        val defaultValue: Any?
    )

    companion object {
        private val LOG_TAG = SettingsImpl::class.simpleName

        private val DEFAULT_AUTO_LOCK_DELAY = TimeUnit.MINUTES
            .toMillis(5)
            .toInt()

        private val PREFS = listOf(
            // Boolean prefs
            Pref(
                keyId = R.string.pref_is_external_storage_cache_enabled,
                type = BOOLEAN,
                defaultValue = false
            ),
            Pref(
                keyId = R.string.pref_is_lock_notification_visible,
                type = BOOLEAN,
                defaultValue = true
            ),

            // Int prefs
            Pref(
                keyId = R.string.pref_auto_lock_delay_in_ms,
                type = INT,
                defaultValue = DEFAULT_AUTO_LOCK_DELAY
            ),

            // String prefs
            Pref(
                keyId = R.string.pref_dropbox_auth_token,
                type = STRING,
                defaultValue = null
            )
        )
    }
}