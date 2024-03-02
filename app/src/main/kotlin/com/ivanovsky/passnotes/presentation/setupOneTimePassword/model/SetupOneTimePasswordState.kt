package com.ivanovsky.passnotes.presentation.setupOneTimePassword.model

import androidx.compose.runtime.Immutable

@Immutable
data class SetupOneTimePasswordState(
    val selectedTab: SetupOneTimePasswordTab,
    val code: String,
    val periodProgress: Float,
    val isPeriodProgressVisible: Boolean,
    val customTabState: CustomTabState,
    val urlTabState: UrlTabState
) {

    companion object {
        val DEFAULT = SetupOneTimePasswordState(
            selectedTab = SetupOneTimePasswordTab.CUSTOM,
            code = "",
            periodProgress = 0f,
            isPeriodProgressVisible = false,
            customTabState = CustomTabState(
                secret = "",
                secretError = null,
                isSecretVisible = false,
                types = emptyList(),
                selectedType = "",
                algorithms = emptyList(),
                selectedAlgorithm = "",
                period = "",
                periodError = null,
                counter = "",
                counterError = null,
                length = "",
                lengthError = null,
                isPeriodVisible = false,
                isCounterVisible = false
            ),
            urlTabState = UrlTabState(
                url = "",
                urlError = null,
            )
        )
    }
}