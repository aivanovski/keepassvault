package com.ivanovsky.passnotes.presentation.setupOneTimePassword.model

import androidx.compose.runtime.Immutable

@Immutable
data class SetupOneTimePasswordState(
    val secret: String,
    val secretError: String?,
    val isSecretVisible: Boolean,
    val types: List<String>,
    val selectedType: String,
    val algorithms: List<String>,
    val selectedAlgorithm: String,
    val period: String,
    val periodError: String?,
    val counter: String,
    val counterError: String?,
    val length: String,
    val lengthError: String?,
    val isPeriodVisible: Boolean,
    val isCounterVisible: Boolean
)