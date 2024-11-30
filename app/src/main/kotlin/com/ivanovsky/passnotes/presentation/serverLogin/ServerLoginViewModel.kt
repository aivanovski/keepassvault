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
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.serverLogin.ServerLoginInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.toFileId
import com.ivanovsky.passnotes.presentation.Screens.ServerLoginScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.NavigateBack
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnDoneButtonClicked
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnIgnoreSslValidationStateChanged
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnPasswordChanged
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnPasswordVisibilityChanged
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnSecretUrlStateChanged
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginIntent.OnSshOptionSelected
import com.ivanovsky.passnotes.presentation.serverLogin.model.ServerLoginState
import com.ivanovsky.passnotes.presentation.serverLogin.model.SshOption
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ServerLoginViewModel(
    private val interactor: ServerLoginInteractor,
    private val errorInteractor: ErrorInteractor,
    themeProvider: ThemeProvider,
    private val resourceProvider: ResourceProvider,
    private val router: Router,
    private val args: ServerLoginArgs
) : ViewModel() {

    val theme = themeFlow(themeProvider)
    private val intents = Channel<ServerLoginIntent>()

    val state = MutableStateFlow<ServerLoginState>(ServerLoginState.NotInitialised)
    private var sshKeyFile: FileDescriptor? = null

    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val visibleMenuItems = MutableLiveData(getVisibleMenuItems())

    fun start() {
        if (state.value != ServerLoginState.NotInitialised) {
            return
        }

        viewModelScope.launch {
            intents.receiveAsFlow()
                .onStart { emit(ServerLoginIntent.Init) }
                .collect { intent ->
                    handleIntent(intent)
                }
        }
    }

    fun sendIntent(intent: ServerLoginIntent) {
        if (intent.isImmediate) {
            handleIntent(intent)
        } else {
            viewModelScope.launch {
                intents.send(intent)
            }
        }
    }

    private fun handleIntent(intent: ServerLoginIntent) {
        when (intent) {
            is ServerLoginIntent.Init -> onInit()
            is ServerLoginIntent.OnUrlChanged -> onUrlChanged(intent)
            is ServerLoginIntent.OnUsernameChanged -> onUsernameChanged(intent)
            is OnPasswordChanged -> onPasswordChanged(intent)
            is OnPasswordVisibilityChanged -> onPasswordVisibilityChanged(intent)
            is OnSecretUrlStateChanged -> onSecretUrlStateChanged(intent)
            is OnSshOptionSelected -> onSshOptionSelected(intent)
            is NavigateBack -> navigateBack()
            is OnDoneButtonClicked -> onDoneButtonClicked()
            is OnIgnoreSslValidationStateChanged -> onIgnoreSslValidationStateChanged(intent)
        }
    }

    private fun onInit() {
        state.value = buildNewDataState()
        visibleMenuItems.value = getVisibleMenuItems()
    }

    private fun onUrlChanged(intent: ServerLoginIntent.OnUrlChanged) {
        val currentState = getDataState() ?: return

        state.value = currentState.copy(
            url = intent.url,
            urlError = null
        )
    }

    private fun onUsernameChanged(intent: ServerLoginIntent.OnUsernameChanged) {
        val currentState = getDataState() ?: return

        state.value = currentState.copy(
            username = intent.username
        )
    }

    private fun onPasswordChanged(intent: OnPasswordChanged) {
        val currentState = getDataState() ?: return

        setScreenState(
            currentState.copy(
                password = intent.password
            )
        )
    }

    private fun onPasswordVisibilityChanged(intent: OnPasswordVisibilityChanged) {
        val currentState = getDataState() ?: return

        setScreenState(
            currentState.copy(
                isPasswordVisible = intent.isVisible
            )
        )
    }

    private fun onSecretUrlStateChanged(intent: OnSecretUrlStateChanged) {
        val currentState = getDataState() ?: return

        state.value = currentState.copy(
            isSecretUrlChecked = intent.isChecked
        )
    }

    private fun onIgnoreSslValidationStateChanged(intent: OnIgnoreSslValidationStateChanged) {
        val currentState = getDataState() ?: return

        state.value = currentState.copy(
            isIgnoreSslValidationChecked = intent.isChecked
        )
    }

    private fun navigateBack() {
        router.exit()
    }

    private fun onDoneButtonClicked() {
        val state = getDataState() ?: return

        clearErrors()

        if (!validateFields()) {
            return
        }

        val credentials = getFsCredentials() ?: return

        setScreenState(ServerLoginState.Loading)
        hideKeyboardEvent.call(Unit)

        viewModelScope.launch {
            val authentication = interactor.authenticate(credentials, args.fsAuthority)
            if (authentication.isFailed) {
                val message = errorInteractor.processAndGetMessage(authentication.error)
                setScreenState(
                    state.copy(
                        errorMessage = message
                    )
                )
                return@launch
            }

            val file = authentication.getOrThrow()

            val save = interactor.saveCredentials(credentials, file.fsAuthority)
            if (save.isFailed) {
                val message = errorInteractor.processAndGetMessage(save.error)
                setScreenState(
                    state.copy(
                        errorMessage = message
                    )
                )
                return@launch
            }

            router.exit()
            router.sendResult(ServerLoginScreen.RESULT_KEY, file)
        }
    }

    private fun getFsCredentials(): FSCredentials? {
        val state = getDataState() ?: return null
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

    private fun onSshOptionSelected(intent: OnSshOptionSelected) {
        if (intent.option is SshOption.Select) {
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
            val currentState = getDataState() ?: return

            setScreenState(
                currentState.copy(
                    isPasswordVisible = (intent.option is SshOption.File),
                    selectedSshOption = intent.option
                )
            )
        }
    }

    private fun onSshKeyFileSelected(file: FileDescriptor) {
        val currentState = getDataState() ?: return

        sshKeyFile = file

        setScreenState(
            currentState.copy(
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
        val currentState = getDataState() ?: return

        setScreenState(
            currentState.copy(
                errorMessage = null,
                urlError = null
            )
        )
    }

    private fun validateFields(): Boolean {
        val currentState = getDataState() ?: return false

        if (currentState.url.isBlank()) {
            setScreenState(
                currentState.copy(
                    urlError = resourceProvider.getString(R.string.empty_field)
                )
            )
        }

        return currentState.url.isNotBlank()
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

    private fun getDataState(): ServerLoginState.Data? {
        return state.value as? ServerLoginState.Data
    }

    private fun buildNewDataState(): ServerLoginState.Data {
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

        return ServerLoginState.Data(
            loginType = args.loginType,
            errorMessage = null,
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
        return if (state.value is ServerLoginState.Data) {
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