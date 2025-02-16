package com.ivanovsky.passnotes.presentation.serverLogin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ScreenStateType
import com.ivanovsky.passnotes.presentation.core.compose.AppDropdownMenu
import com.ivanovsky.passnotes.presentation.core.compose.AppTextField
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.ErrorPanel
import com.ivanovsky.passnotes.presentation.core.compose.ErrorState
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ProgressIndicator
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.cells.ui.InfoCell
import com.ivanovsky.passnotes.presentation.core.compose.contentDescription
import com.ivanovsky.passnotes.presentation.core.compose.model.InputType
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginState
import com.ivanovsky.passnotes.presentation.serverLogin.model.SshOption

@Composable
fun ServerLoginScreen(viewModel: ServerLoginViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ServerLoginScreen(
        state = state,
        onUrlChanged = viewModel::onUrlChanged,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onPasswordVisibilityChanged = viewModel::onPasswordVisibilityChanged,
        onSshOptionSelected = viewModel::onSshOptionSelected,
        onSecretUrlStateChanged = viewModel::onSecretUrlStateChanged,
        onIgnoreSslValidationStateChanged = viewModel::onIgnoreSslValidationStateChanged
    )
}

@Composable
private inline fun <T> rememberCallback(
    crossinline block: (T) -> Unit
): (T) -> Unit {
    return remember { { value -> block.invoke(value) } }
}

@Composable
private inline fun rememberOnClickedCallback(
    crossinline block: () -> Unit
): () -> Unit {
    return remember { { block.invoke() } }
}

@Composable
private fun ServerLoginScreen(
    state: ServerLoginState,
    onUrlChanged: (url: String) -> Unit,
    onUsernameChanged: (username: String) -> Unit,
    onPasswordChanged: (password: String) -> Unit,
    onPasswordVisibilityChanged: (isVisible: Boolean) -> Unit,
    onSshOptionSelected: (option: SshOption) -> Unit,
    onSecretUrlStateChanged: (isChecked: Boolean) -> Unit,
    onIgnoreSslValidationStateChanged: (isChecked: Boolean) -> Unit
) {
    when (state.screenState.type) {
        ScreenStateType.NOT_INITIALIZED, ScreenStateType.LOADING -> {
            ProgressIndicator()
        }

        ScreenStateType.ERROR -> {
            AndroidView(
                factory = { context ->
                    ErrorPanelView(context)
                },
                update = { view ->
                    view.state = state.screenState
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        ScreenStateType.EMPTY -> {
            // TODO:
        }

        ScreenStateType.DATA, ScreenStateType.DATA_WITH_ERROR -> {
            DataContent(
                state = state,
                onUrlChanged = onUrlChanged,
                onUsernameChanged = onUsernameChanged,
                onPasswordChanged = onPasswordChanged,
                onPasswordVisibilityChanged = onPasswordVisibilityChanged,
                onSshOptionSelected = onSshOptionSelected,
                onSecretUrlStateChanged = onSecretUrlStateChanged,
                onIgnoreSslValidationStateChanged = onIgnoreSslValidationStateChanged
            )
        }
    }
}

@Composable
private fun DataContent(
    state: ServerLoginState,
    onUrlChanged: (url: String) -> Unit,
    onUsernameChanged: (username: String) -> Unit,
    onPasswordChanged: (password: String) -> Unit,
    onPasswordVisibilityChanged: (isVisible: Boolean) -> Unit,
    onSshOptionSelected: (option: SshOption) -> Unit,
    onSecretUrlStateChanged: (isChecked: Boolean) -> Unit,
    onIgnoreSslValidationStateChanged: (isChecked: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                ErrorPanelView(context)
            },
            update = { view ->
                view.state = state.screenState
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(color = AppTheme.theme.colors.errorBackground)
        )

        UrlTextField(
            url = state.url,
            urlHint = state.loginType.getUrlHint(),
            urlError = state.urlError,
            onUrlChanged = onUrlChanged
        )

        InfoCell(
            text = state.loginType.getUrlHelpMessage()
        )

        if (state.isSecretUrlCheckboxEnabled) {
            CheckboxItem(
                title = stringResource(R.string.url_contains_secret_message),
                isChecked = state.isSecretUrlChecked,
                onStateChanged = onSecretUrlStateChanged
            )
        }

        if (state.isIgnoreSslValidationCheckboxEnabled) {
            CheckboxItem(
                title = stringResource(R.string.ignore_ssl_certificate_validation),
                isChecked = state.isIgnoreSslValidationChecked,
                onStateChanged = onIgnoreSslValidationStateChanged
            )
        }

        if (state.isSshConfigurationEnabled) {
            SshKeyDropdown(
                selectedSshOption = state.selectedSshOption,
                sshOptions = state.sshOptions,
                onSshOptionSelected = onSshOptionSelected
            )

            if (state.selectedSshOption is SshOption.File) {
                InfoCell(
                    text = stringResource(R.string.ssh_key_copy_message)
                )
            }
        }

        if (state.isUsernameEnabled) {
            UsernameTextField(
                username = state.username,
                onUsernameChanged = onUsernameChanged
            )
        }

        if (state.isPasswordEnabled) {
            PasswordTextField(
                password = state.password,
                passwordHint = state.loginType.getPasswordHint(),
                isPasswordVisible = state.isPasswordVisible,
                onPasswordChanged = onPasswordChanged,
                onPasswordVisibilityChanged = onPasswordVisibilityChanged
            )
        }
    }
}

@Composable
private fun UrlTextField(
    url: String,
    urlHint: String,
    urlError: String?,
    onUrlChanged: (url: String) -> Unit
) {
    AppTextField(
        value = url,
        label = urlHint,
        error = urlError,
        inputType = InputType.TEXT,
        onValueChange = onUrlChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin),
                top = dimensionResource(R.dimen.element_margin)
            )
            .then(contentDescription(stringResource(R.string.url)))
    )
}

@Composable
private fun CheckboxItem(
    title: String,
    isChecked: Boolean,
    onStateChanged: (isChecked: Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = dimensionResource(R.dimen.half_margin))
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onStateChanged,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = dimensionResource(R.dimen.quarter_margin))
        )

        Text(
            text = title,
            style = PrimaryTextStyle(),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = dimensionResource(R.dimen.element_margin))
        )
    }
}

@Composable
private fun SshKeyDropdown(
    selectedSshOption: SshOption,
    sshOptions: List<SshOption>,
    onSshOptionSelected: (option: SshOption) -> Unit
) {
    val selectedOption = selectedSshOption.getTitle()
    val options = sshOptions.map { option -> option.getTitle() }

    val onOptionSelected = rememberCallback { option: String ->
        val index = options.indexOf(option)
        onSshOptionSelected.invoke(sshOptions[index])
    }

    AppDropdownMenu(
        label = stringResource(R.string.ssh_key),
        options = options,
        selectedOption = selectedOption,
        onOptionSelected = onOptionSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(R.dimen.element_margin),
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin)
            )
    )
}

@Composable
private fun UsernameTextField(
    username: String,
    onUsernameChanged: (username: String) -> Unit
) {
    AppTextField(
        value = username,
        label = stringResource(R.string.username),
        inputType = InputType.TEXT,
        onValueChange = onUsernameChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin),
                top = dimensionResource(R.dimen.element_margin)
            )
            .then(contentDescription(stringResource(R.string.username)))
    )
}

@Composable
private fun PasswordTextField(
    password: String,
    passwordHint: String,
    isPasswordVisible: Boolean,
    onPasswordChanged: (password: String) -> Unit,
    onPasswordVisibilityChanged: (isVisible: Boolean) -> Unit
) {
    val onPasswordToggleClicked = rememberOnClickedCallback {
        onPasswordVisibilityChanged.invoke(!isPasswordVisible)
    }

    AppTextField(
        value = password,
        label = passwordHint,
        inputType = InputType.TEXT,
        isPasswordVisible = isPasswordVisible,
        isPasswordToggleEnabled = true,
        onPasswordToggleClicked = onPasswordToggleClicked,
        onValueChange = onPasswordChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin),
                top = dimensionResource(R.dimen.element_margin)
            )
            .then(contentDescription(stringResource(R.string.password)))
    )
}

@Composable
private fun SshOption.getTitle(): String {
    return when (this) {
        is SshOption.NotConfigured -> stringResource(R.string.none)
        is SshOption.Select -> stringResource(R.string.select_file)
        is SshOption.File -> title
    }
}

@Composable
private fun LoginType.getUrlHint(): String {
    return when (this) {
        LoginType.USERNAME_PASSWORD -> stringResource(R.string.webdav_url_hint)
        LoginType.GIT -> stringResource(R.string.git_url_hint)
    }
}

@Composable
private fun LoginType.getPasswordHint(): String {
    return when (this) {
        LoginType.USERNAME_PASSWORD -> stringResource(R.string.password)
        LoginType.GIT -> stringResource(R.string.ssh_key_password)
    }
}

@Composable
private fun LoginType.getUrlHelpMessage(): String {
    return when (this) {
        LoginType.USERNAME_PASSWORD -> stringResource(R.string.webdav_url_help_message)
        LoginType.GIT -> stringResource(R.string.git_url_help_message)
    }
}

@Composable
@Preview
fun LightWebDavPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        ServerLoginScreen(
            state = newWebDavState(),
            onUrlChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onPasswordVisibilityChanged = {},
            onSshOptionSelected = {},
            onSecretUrlStateChanged = {},
            onIgnoreSslValidationStateChanged = {}
        )
    }
}

@Composable
@Preview
fun LightGitPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        ServerLoginScreen(
            state = newGitState(),
            onUrlChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onPasswordVisibilityChanged = {},
            onSshOptionSelected = {},
            onSecretUrlStateChanged = {},
            onIgnoreSslValidationStateChanged = {}
        )
    }
}

@Composable
@Preview
fun DarkGitPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        ServerLoginScreen(
            state = newGitState(),
            onUrlChanged = {},
            onUsernameChanged = {},
            onPasswordChanged = {},
            onPasswordVisibilityChanged = {},
            onSshOptionSelected = {},
            onSecretUrlStateChanged = {},
            onIgnoreSslValidationStateChanged = {}
        )
    }
}

private fun newGitState(): ServerLoginState {
    return ServerLoginState(
        screenState = ScreenState.data(),
        loginType = LoginType.GIT,
        url = "git@example/user/repo.git",
        urlError = "Url error text",
        username = "john.doe",
        password = "abc123",
        isUsernameEnabled = false,
        isPasswordEnabled = true,
        isPasswordVisible = false,
        isSshConfigurationEnabled = true,
        isSecretUrlCheckboxEnabled = true,
        isIgnoreSslValidationCheckboxEnabled = false,
        isSecretUrlChecked = false,
        isIgnoreSslValidationChecked = false,
        selectedSshOption = SshOption.File("id-rsa"),
        sshOptions = emptyList()
    )
}

private fun newWebDavState(): ServerLoginState {
    return ServerLoginState(
        screenState = ScreenState.data(),
        loginType = LoginType.USERNAME_PASSWORD,
        url = "https://example.webdav.com",
        urlError = "Url error text",
        username = "john.doe",
        password = "abc123",
        isUsernameEnabled = true,
        isPasswordEnabled = true,
        isPasswordVisible = false,
        isSshConfigurationEnabled = false,
        isSecretUrlCheckboxEnabled = true,
        isIgnoreSslValidationCheckboxEnabled = true,
        isSecretUrlChecked = false,
        isIgnoreSslValidationChecked = false,
        selectedSshOption = SshOption.NotConfigured,
        sshOptions = emptyList()
    )
}