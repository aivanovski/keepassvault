package com.ivanovsky.passnotes.data.serialization

import com.ivanovsky.passnotes.data.entity.TestToggles
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object TestTogglesConverter {

    private const val KEY_IS_FAKE_BIOMETRIC_ENABLED = "isFakeBiometricModuleEnabled"
    private const val KEY_IS_FAKE_FILE_SYSTEM_ENABLED = "isFakeFileSystemEnabled"

    fun toString(data: TestToggles): String? {
        return try {
            val obj = JSONObject()

            obj.put(KEY_IS_FAKE_BIOMETRIC_ENABLED, data.isFakeBiometricEnabled)
            obj.put(KEY_IS_FAKE_FILE_SYSTEM_ENABLED, data.isFakeFileSystemEnabled)

            obj.toString()
        } catch (exception: JSONException) {
            Timber.d(exception)
            null
        }
    }

    fun fromString(data: String): TestToggles? {
        return try {
            val obj = JSONObject(data)

            val isFakeBiometricEnabled = obj.getBooleanOrNull(KEY_IS_FAKE_BIOMETRIC_ENABLED)
            val isFakeFileSystemEnabled = obj.getBooleanOrNull(KEY_IS_FAKE_FILE_SYSTEM_ENABLED)

            if (isFakeBiometricEnabled != null || isFakeFileSystemEnabled != null) {
                TestToggles(
                    isFakeBiometricEnabled = isFakeBiometricEnabled ?: false,
                    isFakeFileSystemEnabled = isFakeFileSystemEnabled ?: false
                )
            } else {
                null
            }
        } catch (exception: JSONException) {
            null
        }
    }

    private fun JSONObject.getBooleanOrNull(key: String): Boolean? {
        return if (this.has(key)) {
            this.getBoolean(key)
        } else {
            null
        }
    }
}