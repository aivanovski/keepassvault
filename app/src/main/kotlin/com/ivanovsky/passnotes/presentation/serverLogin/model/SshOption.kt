package com.ivanovsky.passnotes.presentation.serverLogin.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface SshOption {

    @Immutable
    data object NotConfigured : SshOption

    @Immutable
    data object Select : SshOption

    @Immutable
    data class File(val title: String) : SshOption
}