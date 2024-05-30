package com.ivanovsky.passnotes.presentation.serverLogin.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ServerLoginState {

    @Immutable
    data object NotInitialised : ServerLoginState

    @Immutable
    data object Loading : ServerLoginState

    @Immutable
    data class Error(
        val message: String
    ) : ServerLoginState

    @Immutable
    data class Data(
        val loginType: LoginType,
        val errorMessage: String?,
        val url: String,
        val urlError: String?,
        val username: String,
        val password: String,
        val isUsernameEnabled: Boolean,
        val isPasswordEnabled: Boolean,
        val isSecretUrlCheckboxEnabled: Boolean,
        val isPasswordVisible: Boolean,
        val isSshConfigurationEnabled: Boolean,
        val isSecretUrlChecked: Boolean,
        val selectedSshOption: SshOption,
        val sshOptions: List<SshOption>
    ) : ServerLoginState
}