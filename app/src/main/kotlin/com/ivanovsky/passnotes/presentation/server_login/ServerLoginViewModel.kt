package com.ivanovsky.passnotes.presentation.server_login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.server_login.ServerLoginInteractor
import com.ivanovsky.passnotes.extensions.getUrl
import com.ivanovsky.passnotes.presentation.Screens.ServerLoginScreen
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.server_login.model.LoginType
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import java.util.UUID

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

        screenState.value = ScreenState.loading()
        hideKeyboardEvent.call()
        isDoneButtonVisible.value = false

        viewModelScope.launch {
            val authentication = interactor.authenticate(credentials, args.fsAuthority)
            if (authentication.isFailed) {
                val message = errorInteractor.processAndGetMessage(authentication.error)
                screenState.value = ScreenState.dataWithError(message)
                isDoneButtonVisible.value = true
                return@launch
            }

            val save = interactor.saveCredentials(credentials, args.fsAuthority)
            if (save.isFailed) {
                val message = errorInteractor.processAndGetMessage(save.error)
                screenState.value = ScreenState.dataWithError(message)
                isDoneButtonVisible.value = true
                return@launch
            }

            val fsAuthority = args.fsAuthority.copy(
                credentials = credentials
            )

            router.sendResult(ServerLoginScreen.RESULT_KEY, fsAuthority)
            router.exit()
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
            else -> null
        }

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

    private fun getUrlHint(loginType: LoginType): String {
        return when (loginType) {
            LoginType.USERNAME_PASSWORD -> resourceProvider.getString(R.string.server_url)
            LoginType.GIT -> resourceProvider.getString(R.string.git_repository_url)
        }
    }
}