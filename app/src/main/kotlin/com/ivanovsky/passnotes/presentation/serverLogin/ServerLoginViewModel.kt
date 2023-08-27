package com.ivanovsky.passnotes.presentation.serverLogin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.serverLogin.ServerLoginInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.getUrl
import com.ivanovsky.passnotes.presentation.Screens.ServerLoginScreen
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.serverLogin.model.LoginType
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.UUID
import kotlinx.coroutines.launch

class ServerLoginViewModel(
    private val interactor: ServerLoginInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val router: Router,
    private val args: ServerLoginArgs
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.data())

    val url = MutableLiveData(EMPTY)
    val urlHint = MutableLiveData(getUrlHint(args.loginType))
    val username = MutableLiveData(EMPTY)
    val password = MutableLiveData(EMPTY)
    val urlError = MutableLiveData<String?>()
    val isSecretUrlChecked = MutableLiveData(true)
    val isDoneButtonVisible = MutableLiveData(true)
    val isUsernameVisible = MutableLiveData(args.loginType == LoginType.USERNAME_PASSWORD)
    val isPasswordVisible = MutableLiveData(args.loginType == LoginType.USERNAME_PASSWORD)
    val isSecretUrlCheckboxVisible = MutableLiveData(args.loginType == LoginType.GIT)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()

    init {
        loadTestCredentials()
    }

    fun authenticate() {
        val url = url.value ?: return
        val username = username.value ?: return
        val password = password.value ?: return

        if (!isFieldsValid(url)) {
            return
        }

        val credentials = when (args.loginType) {
            LoginType.USERNAME_PASSWORD -> FSCredentials.BasicCredentials(
                url = url,
                username = username,
                password = password
            )
            LoginType.GIT -> FSCredentials.GitCredentials(
                url = url,
                isSecretUrl = isSecretUrlChecked.value ?: false,
                salt = UUID.randomUUID().toString()
            )
        }

        setScreenState(ScreenState.loading())
        hideKeyboardEvent.call(Unit)

        viewModelScope.launch {
            val authentication = interactor.authenticate(credentials, args.fsAuthority)
            if (authentication.isFailed) {
                val message = errorInteractor.processAndGetMessage(authentication.error)
                setScreenState(ScreenState.dataWithError(message))
                return@launch
            }

            val file = authentication.getOrThrow()

            val save = interactor.saveCredentials(credentials, file.fsAuthority)
            if (save.isFailed) {
                val message = errorInteractor.processAndGetMessage(save.error)
                setScreenState(ScreenState.dataWithError(message))
                return@launch
            }

            router.exit()
            router.sendResult(ServerLoginScreen.RESULT_KEY, file)
        }
    }

    fun navigateBack() = router.exit()

    private fun isFieldsValid(url: String): Boolean {
        urlError.value = if (url.isBlank()) {
            resourceProvider.getString(R.string.empty_field)
        } else {
            null
        }

        return url.isNotBlank()
    }

    private fun loadTestCredentials() {
        val credentials = when (args.fsAuthority.type) {
            FSType.WEBDAV -> interactor.getTestWebDavCredentials()
            FSType.GIT -> interactor.getTestGitCredentials()
            FSType.FAKE -> interactor.getTestFakeCredentials()
            else -> null
        } ?: return

        when (credentials) {
            is FSCredentials.BasicCredentials -> {
                url.value = credentials.getUrl()
                username.value = credentials.username
                password.value = credentials.password
            }
            is FSCredentials.GitCredentials -> {
                url.value = credentials.url
                isSecretUrlChecked.value = credentials.isSecretUrl
            }
        }
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isDoneButtonVisible.value = !state.isDisplayingLoading
    }

    private fun getUrlHint(loginType: LoginType): String {
        return when (loginType) {
            LoginType.USERNAME_PASSWORD -> resourceProvider.getString(R.string.server_url_hint)
            LoginType.GIT -> resourceProvider.getString(R.string.git_repository_url_hint)
        }
    }
}