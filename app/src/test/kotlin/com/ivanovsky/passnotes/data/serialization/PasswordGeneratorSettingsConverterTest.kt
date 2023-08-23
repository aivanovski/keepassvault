package com.ivanovsky.passnotes.data.serialization

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.data.entity.PasswordGeneratorSettings
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_BRACKETS_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_DIGITS_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_LOWER_CASE_LETTERS_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_MINUS_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_SPACE_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_SPECIAL_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_UNDERSCORE_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_IS_UPPER_CASE_LETTERS_ENABLED
import com.ivanovsky.passnotes.data.serialization.PasswordGeneratorSettingsConverter.KEY_LENGTH
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class PasswordGeneratorSettingsConverterTest {

    @Test
    fun `toString should convert data to json string`() {
        // arrange
        val data = newSettings()

        // act
        val json = PasswordGeneratorSettingsConverter.toString(data)

        // assert
        requireNotNull(json)

        val obj = JSONObject(json)

        val result = PasswordGeneratorSettings(
            length = obj.optInt(KEY_LENGTH),
            isUpperCaseLettersEnabled = obj.optBoolean(KEY_IS_UPPER_CASE_LETTERS_ENABLED),
            isLowerCaseLettersEnabled = obj.optBoolean(KEY_IS_LOWER_CASE_LETTERS_ENABLED),
            isDigitsEnabled = obj.optBoolean(KEY_IS_DIGITS_ENABLED),
            isMinusEnabled = obj.optBoolean(KEY_IS_MINUS_ENABLED),
            isUnderscoreEnabled = obj.optBoolean(KEY_IS_UNDERSCORE_ENABLED),
            isSpaceEnabled = obj.optBoolean(KEY_IS_SPACE_ENABLED),
            isSpecialEnabled = obj.optBoolean(KEY_IS_SPECIAL_ENABLED),
            isBracketsEnabled = obj.optBoolean(KEY_IS_BRACKETS_ENABLED)
        )

        assertThat(result).isEqualTo(data)
    }

    @Test
    fun `fromString should parse json string`() {
        // arrange
        val expected = newSettings()
        val json = JSONObject()
            .apply {
                put(KEY_LENGTH, expected.length)
                put(KEY_IS_UPPER_CASE_LETTERS_ENABLED, expected.isUpperCaseLettersEnabled)
                put(KEY_IS_LOWER_CASE_LETTERS_ENABLED, expected.isLowerCaseLettersEnabled)
                put(KEY_IS_DIGITS_ENABLED, expected.isDigitsEnabled)
                put(KEY_IS_MINUS_ENABLED, expected.isMinusEnabled)
                put(KEY_IS_UNDERSCORE_ENABLED, expected.isUnderscoreEnabled)
                put(KEY_IS_SPACE_ENABLED, expected.isSpaceEnabled)
                put(KEY_IS_SPECIAL_ENABLED, expected.isSpecialEnabled)
                put(KEY_IS_BRACKETS_ENABLED, expected.isBracketsEnabled)
            }
            .toString()

        // act
        val result = PasswordGeneratorSettingsConverter.fromString(json)

        // assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `fromString should return null`() {
        val result = PasswordGeneratorSettingsConverter.fromString("invalid json")
        assertThat(result).isNull()
    }

    private fun newSettings(): PasswordGeneratorSettings =
        PasswordGeneratorSettings(
            length = 12,
            isUpperCaseLettersEnabled = true,
            isLowerCaseLettersEnabled = true,
            isDigitsEnabled = true,
            isMinusEnabled = true,
            isUnderscoreEnabled = true,
            isSpaceEnabled = true,
            isSpecialEnabled = true,
            isBracketsEnabled = true
        )
}