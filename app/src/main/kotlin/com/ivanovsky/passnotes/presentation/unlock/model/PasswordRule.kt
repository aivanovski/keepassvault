package com.ivanovsky.passnotes.presentation.unlock.model

import java.util.regex.Pattern

data class PasswordRule(
    val pattern: Pattern,
    val password: String
)