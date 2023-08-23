package com.ivanovsky.passnotes.data.entity

data class PasswordGeneratorSettings(
    val length: Int,
    val isUpperCaseLettersEnabled: Boolean,
    val isLowerCaseLettersEnabled: Boolean,
    val isDigitsEnabled: Boolean,
    val isMinusEnabled: Boolean,
    val isUnderscoreEnabled: Boolean,
    val isSpaceEnabled: Boolean,
    val isSpecialEnabled: Boolean,
    val isBracketsEnabled: Boolean
) {
    companion object {
        val DEFAULT = PasswordGeneratorSettings(
            length = 12,
            isUpperCaseLettersEnabled = true,
            isLowerCaseLettersEnabled = true,
            isDigitsEnabled = true,
            isMinusEnabled = false,
            isUnderscoreEnabled = true,
            isSpaceEnabled = false,
            isSpecialEnabled = false,
            isBracketsEnabled = false
        )
    }
}