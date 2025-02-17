package com.ivanovsky.passnotes.presentation.serverLogin.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.ScreenState

@Immutable
data class ServerLoginState(
    val screenState: ScreenState,
    val loginType: LoginType,
    val url: String,
    val urlError: String?,
    val username: String,
    val password: String,
    val isUsernameEnabled: Boolean,
    val isPasswordEnabled: Boolean,
    val isSecretUrlCheckboxEnabled: Boolean,
    val isIgnoreSslValidationCheckboxEnabled: Boolean,
    val isPasswordVisible: Boolean,
    val isSshConfigurationEnabled: Boolean,
    val isSecretUrlChecked: Boolean,
    val isIgnoreSslValidationChecked: Boolean,
    val selectedSshOption: SshOption,
    val sshOptions: List<SshOption>
)