package com.ivanovsky.passnotes.presentation.serverLogin.model

sealed class ServerLoginIntent(
    val isImmediate: Boolean
) {

    data object Init : ServerLoginIntent(isImmediate = true)

    data object NavigateBack : ServerLoginIntent(isImmediate = true)

    data object OnDoneButtonClicked : ServerLoginIntent(isImmediate = true)

    data class OnUrlChanged(
        val url: String
    ) : ServerLoginIntent(isImmediate = true)

    data class OnUsernameChanged(
        val username: String
    ) : ServerLoginIntent(isImmediate = true)

    data class OnPasswordChanged(
        val password: String
    ) : ServerLoginIntent(isImmediate = true)

    data class OnPasswordVisibilityChanged(
        val isVisible: Boolean
    ) : ServerLoginIntent(isImmediate = true)

    data class OnSshOptionSelected(
        val option: SshOption
    ) : ServerLoginIntent(isImmediate = true)

    data class OnSecretUrlStateChanged(
        val isChecked: Boolean
    ) : ServerLoginIntent(isImmediate = true)

    data class OnIgnoreSslValidationStateChanged(
        val isChecked: Boolean
    ) : ServerLoginIntent(isImmediate = true)
}