package com.ivanovsky.passnotes.data.repository.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_LOCK_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.DROPBOX_AUTH_TOKEN
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_EXTERNAL_STORAGE_CACHE_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_FILE_LOG_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_LOCK_NOTIFICATION_VISIBLE
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_POSTPONED_SYNC_ENABLED
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
    private val nameResIdToPreferenceMap: Map<Int, Pref> = Pref.values().associateBy { it.keyId }

    override var isExternalStorageCacheEnabled: Boolean
        get() = getBoolean(IS_EXTERNAL_STORAGE_CACHE_ENABLED)
        set(value) {
            putBoolean(IS_EXTERNAL_STORAGE_CACHE_ENABLED, value)
        }

    override var isPostponedSyncEnabled: Boolean
        get() = getBoolean(IS_POSTPONED_SYNC_ENABLED)
        set(value) {
            putBoolean(IS_POSTPONED_SYNC_ENABLED, value)
        }

    override var isLockNotificationVisible: Boolean
        get() = getBoolean(IS_LOCK_NOTIFICATION_VISIBLE)
        set(value) {
            putBoolean(IS_LOCK_NOTIFICATION_VISIBLE, value)
        }

    override var isFileLogEnabled: Boolean
        get() = getBoolean(IS_FILE_LOG_ENABLED)
        set(value) {
            putBoolean(IS_FILE_LOG_ENABLED, value)
        }

    override var autoLockDelayInMs: Int
        get() = getString(AUTO_LOCK_DELAY_IN_MS)?.toInt()
            ?: (AUTO_LOCK_DELAY_IN_MS.defaultValue as String).toInt()
        set(value) {
            putString(AUTO_LOCK_DELAY_IN_MS, value.toString())
        }

    override var autoClearClipboardDelayInMs: Int
        get() = getString(AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS)?.toInt()
            ?: (AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS.defaultValue as String).toInt()
        set(value) {
            putString(AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS, value.toString())
        }

    override var dropboxAuthToken: String?
        get() = getString(DROPBOX_AUTH_TOKEN)
        set(value) {
            putString(DROPBOX_AUTH_TOKEN, value)
        }

    override fun initDefaultIfNeed(pref: Pref) {
        if (preferences.contains(keyFor(pref))) {
            return
        }

        when (pref.type) {
            BOOLEAN -> putBoolean(pref, getDefaultValue(pref))
            INT -> putInt(pref, getDefaultValue(pref))
            STRING -> putString(pref, getDefaultValue(pref))
        }
    }

    fun clean() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun getBoolean(pref: Pref): Boolean {
        return preferences.getBoolean(keyFor(pref), getDefaultValue(pref))
    }

    private fun getString(pref: Pref): String? {
        return preferences.getString(keyFor(pref), getDefaultValue(pref))
    }

    private fun getInt(pref: Pref): Int {
        return preferences.getInt(keyFor(pref), getDefaultValue(pref))
    }

    private fun putBoolean(pref: Pref, value: Boolean) {
        putValue {
            putBoolean(keyFor(pref), value)
        }
    }

    private fun putString(pref: Pref, value: String?) {
        putValue {
            putString(keyFor(pref), value)
        }
    }

    private fun putInt(pref: Pref, value: Int) {
        putValue {
            putInt(keyFor(pref), value)
        }
    }

    private inline fun putValue(action: SharedPreferences.Editor.() -> Unit) {
        val editor = preferences.edit()
        action.invoke(editor)
        editor.apply()
    }

    private fun keyFor(pref: Pref) = resourceProvider.getString(pref.keyId)

    private inline fun <reified T> getDefaultValue(pref: Pref): T {
        return nameResIdToPreferenceMap[pref.keyId]?.defaultValue as T
    }

    enum class PrefType {
        BOOLEAN,
        INT,
        STRING
    }

    enum class Pref(
        @StringRes val keyId: Int,
        val type: PrefType,
        val defaultValue: Any?
    ) {
        // Boolean prefs
        IS_EXTERNAL_STORAGE_CACHE_ENABLED(
            keyId = R.string.pref_is_external_storage_cache_enabled,
            type = BOOLEAN,
            defaultValue = false
        ),
        IS_LOCK_NOTIFICATION_VISIBLE(
            keyId = R.string.pref_is_lock_notification_visible,
            type = BOOLEAN,
            defaultValue = true
        ),
        IS_FILE_LOG_ENABLED(
            keyId = R.string.pref_is_file_log_enabled,
            type = BOOLEAN,
            defaultValue = false
        ),
        IS_POSTPONED_SYNC_ENABLED(
            keyId = R.string.pref_is_postponed_sync_enabled,
            type = BOOLEAN,
            defaultValue = true
        ),

        // Int prefs
        AUTO_LOCK_DELAY_IN_MS(
            keyId = R.string.pref_auto_lock_delay_in_ms,
            type = STRING,
            defaultValue = TimeUnit.MINUTES.toMillis(5).toString()
        ),
        AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS(
            keyId = R.string.pref_auto_clear_clipboard_delay_in_ms,
            type = STRING,
            defaultValue = TimeUnit.SECONDS.toMillis(30).toString()
        ),

        // String prefs
        DROPBOX_AUTH_TOKEN(
            keyId = R.string.pref_dropbox_auth_token,
            type = STRING,
            defaultValue = null
        )
    }
}