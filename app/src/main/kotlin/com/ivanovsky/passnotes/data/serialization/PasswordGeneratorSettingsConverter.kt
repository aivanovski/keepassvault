package com.ivanovsky.passnotes.data.serialization

import com.ivanovsky.passnotes.data.entity.PasswordGeneratorSettings
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object PasswordGeneratorSettingsConverter {

    const val KEY_LENGTH = "length"
    const val KEY_IS_UPPER_CASE_LETTERS_ENABLED = "isUpperCaseLettersEnabled"
    const val KEY_IS_LOWER_CASE_LETTERS_ENABLED = "isLowerCaseLettersEnabled"
    const val KEY_IS_DIGITS_ENABLED = "isDigitsEnabled"
    const val KEY_IS_MINUS_ENABLED = "isMinusEnabled"
    const val KEY_IS_UNDERSCORE_ENABLED = "isUnderscoreEnabled"
    const val KEY_IS_SPACE_ENABLED = "isSpaceEnabled"
    const val KEY_IS_SPECIAL_ENABLED = "isSpecialEnabled"
    const val KEY_IS_BRACKETS_ENABLED = "isBracketsEnabled"

    fun toString(settings: PasswordGeneratorSettings): String? {
        return try {
            val obj = JSONObject()

            obj.put(KEY_LENGTH, settings.length)
            obj.put(KEY_IS_UPPER_CASE_LETTERS_ENABLED, settings.isUpperCaseLettersEnabled)
            obj.put(KEY_IS_LOWER_CASE_LETTERS_ENABLED, settings.isLowerCaseLettersEnabled)
            obj.put(KEY_IS_DIGITS_ENABLED, settings.isDigitsEnabled)
            obj.put(KEY_IS_MINUS_ENABLED, settings.isMinusEnabled)
            obj.put(KEY_IS_UNDERSCORE_ENABLED, settings.isUnderscoreEnabled)
            obj.put(KEY_IS_SPACE_ENABLED, settings.isSpaceEnabled)
            obj.put(KEY_IS_SPECIAL_ENABLED, settings.isSpecialEnabled)
            obj.put(KEY_IS_BRACKETS_ENABLED, settings.isBracketsEnabled)

            obj.toString()
        } catch (e: JSONException) {
            Timber.d(e)
            null
        }
    }

    fun fromString(data: String): PasswordGeneratorSettings? {
        return try {
            val obj = JSONObject(data)

            val length = obj.optInt(KEY_LENGTH, -1)
            if (length == -1) {
                return null
            }

            val isUpperCaseLettersEnabled = obj.optBoolean(KEY_IS_UPPER_CASE_LETTERS_ENABLED, false)
            val isLowerCaseLettersEnabled = obj.optBoolean(KEY_IS_LOWER_CASE_LETTERS_ENABLED, false)
            val isDigitsEnabled = obj.optBoolean(KEY_IS_DIGITS_ENABLED, false)
            val isMinusEnabled = obj.optBoolean(KEY_IS_MINUS_ENABLED, false)
            val isUnderscoreEnabled = obj.optBoolean(KEY_IS_UNDERSCORE_ENABLED, false)
            val isSpaceEnabled = obj.optBoolean(KEY_IS_SPACE_ENABLED, false)
            val isSpecialEnabled = obj.optBoolean(KEY_IS_SPECIAL_ENABLED, false)
            val isBracketsEnabled = obj.optBoolean(KEY_IS_BRACKETS_ENABLED, false)

            PasswordGeneratorSettings(
                length = length,
                isUpperCaseLettersEnabled = isUpperCaseLettersEnabled,
                isLowerCaseLettersEnabled = isLowerCaseLettersEnabled,
                isDigitsEnabled = isDigitsEnabled,
                isMinusEnabled = isMinusEnabled,
                isUnderscoreEnabled = isUnderscoreEnabled,
                isSpaceEnabled = isSpaceEnabled,
                isSpecialEnabled = isSpecialEnabled,
                isBracketsEnabled = isBracketsEnabled
            )
        } catch (e: JSONException) {
            Timber.d(e)
            null
        }
    }
}