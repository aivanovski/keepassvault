package com.ivanovsky.passnotes.data.serialization

import com.ivanovsky.passnotes.data.entity.TestToggles
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object TestTogglesConverter {

    private const val KEY_IS_FAKE_BIOMETRIC_ENABLED = "isFakeBiometricModuleEnabled"

    fun toString(data: TestToggles): String? {
        return try {
            val obj = JSONObject()

            obj.put(KEY_IS_FAKE_BIOMETRIC_ENABLED, data.isFakeBiometricEnabled)

            obj.toString()
        } catch (exception: JSONException) {
            Timber.d(exception)
            null
        }
    }

    fun fromString(data: String): TestToggles? {
        return try {
            val obj = JSONObject(data)

            val isFakeBiometricEnabled = if (obj.has(KEY_IS_FAKE_BIOMETRIC_ENABLED)) {
                obj.getBoolean(KEY_IS_FAKE_BIOMETRIC_ENABLED)
            } else {
                null
            }

            if (isFakeBiometricEnabled != null) {
                TestToggles(
                    isFakeBiometricEnabled = isFakeBiometricEnabled
                )
            } else {
                null
            }
        } catch (exception: JSONException) {
            null
        }
    }
}