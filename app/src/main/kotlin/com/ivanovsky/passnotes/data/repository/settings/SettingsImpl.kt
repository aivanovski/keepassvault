package com.ivanovsky.passnotes.data.repository.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.PasswordGeneratorSettings
import com.ivanovsky.passnotes.data.entity.TestAutofillData
import com.ivanovsky.passnotes.data.entity.TestToggles
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_CLEAR_CLIPBOARD_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.AUTO_LOCK_DELAY_IN_MS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.GIT_USER_EMAIL
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.GIT_USER_NAME
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_BIOMETRIC_UNLOCK_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_EXTERNAL_STORAGE_CACHE_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_FILE_LOG_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_GROUPS_AT_START_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_LOCK_NOTIFICATION_DIALOG_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_LOCK_NOTIFICATION_VISIBLE
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_POSTPONED_SYNC_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.IS_SSL_CERTIFICATE_VALIDATION_ENABLED
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.PASSWORD_GENERATOR_SETTINGS
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.SEARCH_TYPE
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.SORT_DIRECTION
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.SORT_TYPE
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.TEST_AUTOFILL_DATA
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.Pref.TEST_TOGGLES
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.PrefType.BOOLEAN
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.PrefType.INT
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl.PrefType.STRING
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter
import com.ivanovsky.passnotes.data.serialization.TestAutofillDataConverter
import com.ivanovsky.passnotes.data.serialization.TestTogglesConverter
import com.ivanovsky.passnotes.domain.entity.SearchType
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class SettingsImpl(private val context: Context) : Settings {

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<OnSettingsChangeListener>()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val nameResIdToPreferenceMap: Map<Int, Pref> = Pref.values().associateBy { it.keyId }
    private val keyToPreferenceMap: Map<String, Pref> = Pref.values()
        .associateBy { context.getString(it.keyId) }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null) {
            onPreferenceChanged(key)
        }
    }

    override var isExternalStorageCacheEnabled: Boolean
        get() = getBoolean(IS_EXTERNAL_STORAGE_CACHE_ENABLED)
        set(value) {
            putBoolean(IS_EXTERNAL_STORAGE_CACHE_ENABLED, value)
        }

    override var isSslCertificateValidationEnabled: Boolean
        get() = getBoolean(IS_SSL_CERTIFICATE_VALIDATION_ENABLED)
        set(value) {
            putBoolean(IS_SSL_CERTIFICATE_VALIDATION_ENABLED, value)
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

    override var isLockNotificationDialogEnabled: Boolean
        get() = getBoolean(IS_LOCK_NOTIFICATION_DIALOG_ENABLED)
        set(value) {
            putBoolean(IS_LOCK_NOTIFICATION_DIALOG_ENABLED, value)
        }

    override var isFileLogEnabled: Boolean
        get() = getBoolean(IS_FILE_LOG_ENABLED)
        set(value) {
            putBoolean(IS_FILE_LOG_ENABLED, value)
        }

    override var isBiometricUnlockEnabled: Boolean
        get() = getBoolean(IS_BIOMETRIC_UNLOCK_ENABLED)
        set(value) {
            putBoolean(IS_BIOMETRIC_UNLOCK_ENABLED, value)
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

    override var searchType: SearchType
        get() {
            return getString(SEARCH_TYPE)?.let { SearchType.getByName(it) }
                ?: SearchType.default()
        }
        set(value) {
            putString(SEARCH_TYPE, value.name)
        }

    override var sortType: SortType
        get() {
            return getString(SORT_TYPE)?.let { SortType.getByName(it) }
                ?: SortType.default()
        }
        set(value) {
            putString(SORT_TYPE, value.name)
        }

    override var sortDirection: SortDirection
        get() {
            return getString(SORT_DIRECTION)?.let { SortDirection.getByName(it) }
                ?: SortDirection.default()
        }
        set(value) {
            putString(SORT_DIRECTION, value.name)
        }

    override var isGroupsAtStartEnabled: Boolean
        get() = getBoolean(IS_GROUPS_AT_START_ENABLED)
        set(value) {
            putBoolean(IS_GROUPS_AT_START_ENABLED, value)
        }

    override var passwordGeneratorSettings: PasswordGeneratorSettings
        get() = getString(PASSWORD_GENERATOR_SETTINGS)?.let {
            PasswordGeneratorSettingsConverter.fromString(it)
        } ?: PasswordGeneratorSettings.DEFAULT
        set(value) {
            val strValue = value.let { PasswordGeneratorSettingsConverter.toString(it) }
            putString(PASSWORD_GENERATOR_SETTINGS, strValue)
        }

    override var gitUserName: String?
        get() = getString(GIT_USER_NAME)
        set(value) {
            putString(GIT_USER_NAME, value?.ifEmpty { null })
        }

    override var gitUserEmail: String?
        get() = getString(GIT_USER_EMAIL)
        set(value) {
            putString(GIT_USER_EMAIL, value?.ifEmpty { null })
        }

    override var testAutofillData: TestAutofillData?
        get() = getString(TEST_AUTOFILL_DATA)?.let { TestAutofillDataConverter.fromString(it) }
        set(value) {
            putString(TEST_AUTOFILL_DATA, value?.let { TestAutofillDataConverter.toString(it) })
        }

    override var testToggles: TestToggles?
        get() = getString(TEST_TOGGLES)?.let { TestTogglesConverter.fromString(it) }
        set(value) {
            putString(TEST_TOGGLES, value?.let { TestTogglesConverter.toString(it) })
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
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

    override fun register(listener: OnSettingsChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun unregister(listener: OnSettingsChangeListener) {
        listeners.remove(listener)
    }

    fun clean() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun onPreferenceChanged(key: String) {
        val pref = keyToPreferenceMap[key] ?: return

        listeners.forEach { listener ->
            handler.post { listener.onSettingsChanged(pref) }
        }
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

    private fun keyFor(pref: Pref) = context.getString(pref.keyId)

    private inline fun <reified T> getDefaultValue(pref: Pref): T {
        return nameResIdToPreferenceMap[pref.keyId]?.defaultValue as T
    }

    enum class PrefType {
        BOOLEAN,
        INT,
        STRING
    }

    // TODO: Refactor, rename and move to separate file
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
        IS_SSL_CERTIFICATE_VALIDATION_ENABLED(
            keyId = R.string.pref_is_ssl_certificate_validation_enabled,
            type = BOOLEAN,
            defaultValue = true
        ),
        IS_LOCK_NOTIFICATION_VISIBLE(
            keyId = R.string.pref_is_lock_notification_visible,
            type = BOOLEAN,
            defaultValue = true
        ),
        IS_LOCK_NOTIFICATION_DIALOG_ENABLED(
            keyId = R.string.pref_is_lock_notification_dialog_enabled,
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
        IS_GROUPS_AT_START_ENABLED(
            keyId = R.string.pref_is_groups_at_start_enabled,
            type = BOOLEAN,
            defaultValue = true
        ),
        IS_BIOMETRIC_UNLOCK_ENABLED(
            keyId = R.string.pref_is_biometric_unlock_enabled,
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
        SEARCH_TYPE(
            keyId = R.string.pref_search_type,
            type = STRING,
            defaultValue = SearchType.default().name
        ),
        SORT_TYPE(
            keyId = R.string.pref_sort_type,
            type = STRING,
            defaultValue = null
        ),
        SORT_DIRECTION(
            keyId = R.string.pref_sort_direction,
            type = STRING,
            defaultValue = null
        ),
        PASSWORD_GENERATOR_SETTINGS(
            keyId = R.string.pref_password_generator_settings,
            type = STRING,
            defaultValue = null
        ),
        TEST_AUTOFILL_DATA(
            keyId = R.string.pref_test_autofill_data,
            type = STRING,
            defaultValue = null
        ),
        TEST_TOGGLES(
            keyId = R.string.pref_test_toggles,
            type = STRING,
            defaultValue = null
        ),
        GIT_USER_NAME(
            keyId = R.string.pref_git_user_name,
            type = STRING,
            defaultValue = null
        ),
        GIT_USER_EMAIL(
            keyId = R.string.pref_git_user_email,
            type = STRING,
            defaultValue = null
        )
    }
}