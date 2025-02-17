package com.ivanovsky.passnotes.presentation.serverLogin

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.serverLogin.ServerLoginInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.toFileId
import com.ivanovsky.passnotes.presentation.Screens.ServerLoginScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginState
import com.ivanovsky.passnotes.presentation.serverLogin.model.SshOption
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ServerLoginViewModel(
    private val interactor: ServerLoginInteractor,
    themeProvider: ThemeProvider,
    private val resourceProvider: ResourceProvider,
    private val router: Router,
    private val args: ServerLoginArgs
) : ViewModel() {

    val theme = themeFlow(themeProvider)

    val state = MutableStateFlow(createInitialState())
    private var sshKeyFile: FileDescriptor? = null

    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val visibleMenuItems = MutableLiveData(getVisibleMenuItems())

    fun onUrlChanged(url: String) {
        state.value = state.value.copy(
            url = url,
            urlError = null
        )
    }

    fun onUsernameChanged(username: String) {
        state.value = state.value.copy(
            username = username
        )
    }

    fun onPasswordChanged(password: String) {
        setScreenState(
            state.value.copy(
                password = password
            )
        )
    }

    fun onPasswordVisibilityChanged(isVisible: Boolean) {
        setScreenState(
            state.value.copy(
                isPasswordVisible = isVisible
            )
        )
    }

    fun onSecretUrlStateChanged(isChecked: Boolean) {
        state.value = state.value.copy(
            isSecretUrlChecked = isChecked
        )
    }

    fun onIgnoreSslValidationStateChanged(isChecked: Boolean) {
        state.value = state.value.copy(
            isIgnoreSslValidationChecked = isChecked
        )
    }

    fun navigateBack() {
        router.exit()
    }

    fun onDoneButtonClicked() {
        val state = state.value

        clearErrors()

        if (!validateFields()) {
            return
        }

        val credentials = getFsCredentials() ?: return

        setScreenState(
            state.copy(
                screenState = ScreenState.loading()
            )
        )
        hideKeyboardEvent.call(Unit)

        viewModelScope.launch {
            val authentication = interactor.authenticate(credentials, args.fsAuthority)
            if (authentication.isFailed) {
                setScreenState(
                    state.copy(
                        screenState = ScreenState.dataWithError(authentication.error),
                    )
                )
                return@launch
            }

            val file = authentication.getOrThrow()

            val save = interactor.saveCredentials(credentials, file.fsAuthority)
            if (save.isFailed) {
                setScreenState(
                    state.copy(
                        screenState = ScreenState.dataWithError(save.error),
                    )
                )
                return@launch
            }

            router.exit()
            router.sendResult(ServerLoginScreen.RESULT_KEY, file)
        }
    }

    private fun getFsCredentials(): FSCredentials {
        val state = state.value
        val sshKeyFile = this.sshKeyFile

        val isSshFileSpecified = (state.selectedSshOption is SshOption.File)
        val loginType = args.loginType
        val isIgnoreSslValidation = (
            state.isIgnoreSslValidationCheckboxEnabled &&
                state.isIgnoreSslValidationChecked
            )

        return when {
            loginType == LoginType.GIT && isSshFileSpecified && sshKeyFile != null -> {
                FSCredentials.SshCredentials(
                    url = state.url.trim(),
                    isSecretUrl = state.isSecretUrlChecked,
                    salt = UUID.randomUUID().toString(),
                    keyFile = sshKeyFile.toFileId(),
                    password = state.password.trim()
                )
            }

            loginType == LoginType.GIT -> {
                FSCredentials.GitCredentials(
                    url = state.url.trim(),
                    isSecretUrl = state.isSecretUrlChecked,
                    salt = UUID.randomUUID().toString()
                )
            }

            else -> {
                FSCredentials.BasicCredentials(
                    url = state.url.trim(),
                    username = state.username.trim(),
                    password = state.password.trim(),
                    isIgnoreSslValidation = isIgnoreSslValidation
                )
            }
        }
    }

    fun onSshOptionSelected(option: SshOption) {
        if (option is SshOption.Select) {
            viewModelScope.launch {
                // The delay is necessary for ExposedDropdownMenu,
                // otherwise it produces crash
                delay(500L)

                val resultKey = StorageListScreen.newResultKey()

                router.setResultListener(resultKey) { file ->
                    if (file is FileDescriptor) {
                        onSshKeyFileSelected(file)
                    }
                }

                router.navigateTo(
                    StorageListScreen(
                        StorageListArgs(
                            action = Action.PICK_FILE,
                            resultKey = resultKey
                        )
                    )
                )
            }
        } else {
            setScreenState(
                state.value.copy(
                    isPasswordVisible = (option is SshOption.File),
                    selectedSshOption = option
                )
            )
        }
    }

    private fun onSshKeyFileSelected(file: FileDescriptor) {
        sshKeyFile = file

        setScreenState(
            state.value.copy(
                isPasswordEnabled = true,
                selectedSshOption = SshOption.File(file.name),
                sshOptions = listOf(
                    SshOption.File(file.name),
                    SshOption.NotConfigured,
                    SshOption.Select
                )
            )
        )
    }

    private fun clearErrors() {
        setScreenState(
            state.value.copy(
                screenState = ScreenState.data(),
                urlError = null
            )
        )
    }

    private fun validateFields(): Boolean {
        if (state.value.url.isBlank()) {
            setScreenState(
                state.value.copy(
                    urlError = resourceProvider.getString(R.string.empty_field)
                )
            )
        }

        return state.value.url.isNotBlank()
    }

    private fun getCredentialsToFill(): FSCredentials? {
        val credentialsType = args.fsAuthority.type
        val oldCredentials = args.fsAuthority.credentials

        return when {
            oldCredentials is FSCredentials.BasicCredentials -> {
                oldCredentials.copy(
                    password = EMPTY
                )
            }

            oldCredentials is FSCredentials.GitCredentials -> oldCredentials
            credentialsType == FSType.WEBDAV -> interactor.getTestWebDavCredentials()
            credentialsType == FSType.GIT -> interactor.getTestGitCredentials()
            credentialsType == FSType.FAKE -> interactor.getTestFakeCredentials()
            else -> null
        }
    }

    private fun createInitialState(): ServerLoginState {
        val creds = getCredentialsToFill()

        val url = when (creds) {
            is FSCredentials.BasicCredentials -> creds.url
            is FSCredentials.GitCredentials -> creds.url
            is FSCredentials.SshCredentials -> creds.url
            else -> ""
        }

        val username = when (creds) {
            is FSCredentials.BasicCredentials -> creds.username
            else -> ""
        }

        val password = when (creds) {
            is FSCredentials.BasicCredentials -> creds.password
            else -> ""
        }

        return ServerLoginState(
            screenState = ScreenState.data(),
            loginType = args.loginType,
            url = url,
            urlError = null,
            username = username,
            password = password,
            isUsernameEnabled = isUsernameVisible(),
            isPasswordEnabled = isPasswordVisible(),
            isSecretUrlCheckboxEnabled = isSecretUrlCheckboxEnabled(),
            isIgnoreSslValidationCheckboxEnabled = isIgnoreSslValidationCheckboxEnabled(),
            isPasswordVisible = false,
            isSshConfigurationEnabled = isSshConfigurationEnabled(),
            isSecretUrlChecked = false,
            isIgnoreSslValidationChecked = false,
            selectedSshOption = SshOption.NotConfigured,
            sshOptions = listOf(SshOption.NotConfigured, SshOption.Select)
        )
    }

    private fun isUsernameVisible(): Boolean {
        return args.loginType == LoginType.USERNAME_PASSWORD
    }

    private fun isPasswordVisible(): Boolean {
        return args.loginType == LoginType.USERNAME_PASSWORD
    }

    private fun isSshConfigurationEnabled(): Boolean {
        return args.loginType == LoginType.GIT
    }

    private fun isSecretUrlCheckboxEnabled(): Boolean {
        return args.loginType == LoginType.GIT
    }

    private fun isIgnoreSslValidationCheckboxEnabled(): Boolean {
        return args.loginType == LoginType.USERNAME_PASSWORD && BuildConfig.DEBUG
    }

    private fun getVisibleMenuItems(): List<ServerLoginMenuItem> {
        return if (state.value.screenState.isDisplayingData) {
            ServerLoginMenuItem.entries
        } else {
            emptyList()
        }
    }

    private fun setScreenState(state: ServerLoginState) {
        this.state.value = state
        visibleMenuItems.value = getVisibleMenuItems()
    }

    enum class ServerLoginMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        DONE(R.id.menu_done)
    }
}