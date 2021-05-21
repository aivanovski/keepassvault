package com.ivanovsky.passnotes.presentation.server_login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.server_login.ServerLoginInteractor
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch

class ServerLoginViewModel(
    private val interactor: ServerLoginInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val args: ServerLoginArgs
) : ViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.data())

    val url = MutableLiveData(EMPTY)
    val username = MutableLiveData(EMPTY)
    val password = MutableLiveData(EMPTY)
    val urlError = MutableLiveData<String?>()
    val doneButtonVisibility = MutableLiveData(true)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val finishScreenEvent = SingleLiveEvent<FSAuthority>()

    init {
        loadDebugData()
    }

    fun authenticate() {
        val url = url.value ?: return
        val username = username.value ?: return
        val password = password.value ?: return

        if (!isFieldsValid(url)) {
            return
        }

        val credentials = ServerCredentials(url, username, password)

        screenState.value = ScreenState.loading()
        hideKeyboardEvent.call()
        doneButtonVisibility.value = false

        viewModelScope.launch {
            val authentication = interactor.authenticate(credentials, args.fsAuthority)
            if (authentication.isFailed) {
                val message = errorInteractor.processAndGetMessage(authentication.error)
                screenState.value = ScreenState.dataWithError(message)
                doneButtonVisibility.value = true
                return@launch
            }

            val save = interactor.saveCredentials(credentials, args.fsAuthority)
            if (save.isFailed) {
                val message = errorInteractor.processAndGetMessage(save.error)
                screenState.value = ScreenState.dataWithError(message)
                doneButtonVisibility.value = true
                return@launch
            }

            val fsAuthority = args.fsAuthority.copy(
                credentials = credentials
            )
            finishScreenEvent.call(fsAuthority)
        }
    }

    private fun isFieldsValid(
        url: String
    ): Boolean {
        urlError.value = if (url.isBlank()) {
            resourceProvider.getString(R.string.empty_field)
        } else {
            null
        }

        return url.isNotBlank()
    }

    private fun loadDebugData() {
        val credentials = when (args.fsAuthority.type) {
            FSType.WEBDAV -> interactor.getDebugWebDavCredentials()
            else -> null
        }

        credentials?.let {
            url.value = it.serverUrl
            username.value = it.username
            password.value = it.password
        }
    }
}