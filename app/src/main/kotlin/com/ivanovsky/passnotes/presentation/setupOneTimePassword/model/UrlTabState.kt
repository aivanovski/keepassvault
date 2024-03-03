package com.ivanovsky.passnotes.presentation.setupOneTimePassword.model

import androidx.compose.runtime.Immutable

@Immutable
data class UrlTabState(
    val url: String,
    val urlError: String?
)