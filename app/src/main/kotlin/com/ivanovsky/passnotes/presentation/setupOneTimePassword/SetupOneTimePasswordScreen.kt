package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppDropdownMenu
import com.ivanovsky.passnotes.presentation.core.compose.AppTextField
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.HeaderTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.PrimaryTextStyle
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.model.InputType
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.CustomTabState
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.SetupOneTimePasswordState
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.SetupOneTimePasswordTab
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.UrlTabState

@Composable
fun SetupOneTimePasswordScreen(
    viewModel: SetupOneTimePasswordViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle(SetupOneTimePasswordState.DEFAULT)
    val progress by viewModel.periodProgress.collectAsStateWithLifecycle(0f)

    SetupOneTimePasswordScreen(
        state = state,
        periodProgressProvider = { progress },
        onTabSelected = viewModel::onTabChanged,
        onTypeChanged = viewModel::onTypeChanged,
        onAlgorithmChanged = viewModel::onAlgorithmChanged,
        onPeriodChanged = viewModel::onPeriodChanged,
        onCounterChanged = viewModel::onCounterChanged,
        onLengthChanged = viewModel::onLengthChanged,
        onSecretChanged = viewModel::onSecretChanged,
        onSecretVisibilityChanged = viewModel::onSecretVisibilityChanged,
        onUrlChanged = viewModel::onUrlChanged
    )
}

@Composable
private fun SetupOneTimePasswordScreen(
    state: SetupOneTimePasswordState,
    periodProgressProvider: () -> Float,
    onTabSelected: (tab: SetupOneTimePasswordTab) -> Unit,
    onTypeChanged: (type: String) -> Unit,
    onAlgorithmChanged: (algorithm: String) -> Unit,
    onPeriodChanged: (period: String) -> Unit,
    onCounterChanged: (counter: String) -> Unit,
    onLengthChanged: (length: String) -> Unit,
    onSecretChanged: (secret: String) -> Unit,
    onSecretVisibilityChanged: () -> Unit,
    onUrlChanged: (url: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        CodeContent(
            code = state.code,
            periodProgressProvider = periodProgressProvider,
            isPeriodProgressVisible = state.isPeriodProgressVisible
        )

        TabsContent(
            tabs = SetupOneTimePasswordTab.entries,
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )

        when (state.selectedTab) {
            SetupOneTimePasswordTab.CUSTOM -> {
                CustomTabContent(
                    state = state.customTabState,
                    onTypeChanged = onTypeChanged,
                    onAlgorithmChanged = onAlgorithmChanged,
                    onPeriodChanged = onPeriodChanged,
                    onCounterChanged = onCounterChanged,
                    onLengthChanged = onLengthChanged,
                    onSecretChanged = onSecretChanged,
                    onSecretVisibilityChanged = onSecretVisibilityChanged
                )
            }

            SetupOneTimePasswordTab.URL -> {
                UrlTabContent(
                    state = state.urlTabState,
                    onUrlChanged = onUrlChanged
                )
            }
        }
    }
}

@Composable
private fun CodeContent(
    code: String,
    periodProgressProvider: () -> Float,
    isPeriodProgressVisible: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.theme.colors.surface
        ),
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(R.dimen.element_margin),
                vertical = dimensionResource(R.dimen.element_margin)
            )
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(R.dimen.element_margin)
                )
        ) {
            Text(
                text = stringResource(
                    R.string.text_with_colon,
                    stringResource(R.string.your_code_is)
                ),
                style = PrimaryTextStyle(),
                textAlign = TextAlign.Center,
                modifier = Modifier
            )

            Row(
                modifier = Modifier
                    .padding(
                        top = dimensionResource(R.dimen.element_margin)
                    )
            ) {
                Text(
                    style = HeaderTextStyle(),
                    textAlign = TextAlign.Center,
                    text = code,
                    modifier = Modifier
                        .sizeIn(minHeight = dimensionResource(R.dimen.medium_progress_bar_size))
                )

                if (isPeriodProgressVisible) {
                    CircularProgressIndicator(
                        progress = periodProgressProvider,
                        color = AppTheme.theme.colors.progressSecondary,
                        modifier = Modifier
                            .padding(
                                start = dimensionResource(R.dimen.half_margin)
                            )
                            .size(dimensionResource(R.dimen.medium_progress_bar_size))
                    )
                }
            }
        }
    }
}

@Composable
private fun TabsContent(
    tabs: List<SetupOneTimePasswordTab>,
    selectedTab: SetupOneTimePasswordTab,
    onTabSelected: (tab: SetupOneTimePasswordTab) -> Unit
) {
    val selectedTabIndex = tabs.indexOf(selectedTab)

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = AppTheme.theme.colors.background,
        modifier = Modifier
            .padding(
                top = dimensionResource(R.dimen.half_margin)
            )
    ) {
        for (tab in tabs) {
            Tab(
                selected = (tab == selectedTab),
                modifier = Modifier
                    .height(dimensionResource(R.dimen.tab_panel_height)),
                onClick = { onTabSelected.invoke(tab) }
            ) {
                val title = when (tab) {
                    SetupOneTimePasswordTab.CUSTOM -> stringResource(R.string.custom)
                    SetupOneTimePasswordTab.URL -> stringResource(R.string.url_cap)
                }

                Text(
                    style = PrimaryTextStyle(),
                    text = title
                )
            }
        }
    }
}

@Composable
private fun CustomTabContent(
    state: CustomTabState,
    onTypeChanged: (type: String) -> Unit,
    onAlgorithmChanged: (algorithm: String) -> Unit,
    onPeriodChanged: (period: String) -> Unit,
    onCounterChanged: (counter: String) -> Unit,
    onLengthChanged: (length: String) -> Unit,
    onSecretChanged: (secret: String) -> Unit,
    onSecretVisibilityChanged: () -> Unit
) {
    AppTextField(
        value = state.secret,
        label = stringResource(R.string.secret),
        error = state.secretError,
        inputType = InputType.TEXT,
        isPasswordToggleEnabled = true,
        isPasswordVisible = state.isSecretVisible,
        onPasswordToggleClicked = onSecretVisibilityChanged,
        onValueChange = { newSecret -> onSecretChanged.invoke(newSecret) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin),
                top = dimensionResource(R.dimen.element_margin)
            )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin),
                top = dimensionResource(R.dimen.element_margin)
            )
    ) {
        AppDropdownMenu(
            label = stringResource(R.string.type),
            options = state.types,
            selectedOption = state.selectedType,
            onOptionSelected = { newType -> onTypeChanged.invoke(newType) },
            modifier = Modifier
                .padding(end = dimensionResource(R.dimen.half_margin))
                .weight(weight = 0.5f)
        )

        AppDropdownMenu(
            label = stringResource(R.string.algorithm),
            options = state.algorithms,
            selectedOption = state.selectedAlgorithm,
            onOptionSelected = { newAlgorithm -> onAlgorithmChanged.invoke(newAlgorithm) },
            modifier = Modifier
                .padding(start = dimensionResource(R.dimen.half_margin))
                .weight(weight = 0.5f)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(R.dimen.element_margin),
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin)
            )
    ) {
        if (state.isPeriodVisible) {
            AppTextField(
                value = state.period,
                label = stringResource(R.string.generation_interval),
                error = state.periodError,
                inputType = InputType.NUMBER,
                maxLength = 3,
                isPasswordToggleEnabled = false,
                onValueChange = { newPeriod -> onPeriodChanged.invoke(newPeriod) },
                modifier = Modifier
                    .padding(
                        end = dimensionResource(R.dimen.half_margin)
                    )
                    .weight(weight = 0.5f)
            )
        }

        if (state.isCounterVisible) {
            AppTextField(
                value = state.counter,
                label = stringResource(R.string.counter),
                error = state.counterError,
                inputType = InputType.NUMBER,
                isPasswordToggleEnabled = false,
                onValueChange = { newCounter -> onCounterChanged.invoke(newCounter) },
                modifier = Modifier
                    .padding(
                        end = dimensionResource(R.dimen.half_margin)
                    )
                    .weight(weight = 0.5f)
            )
        }

        AppTextField(
            value = state.length,
            label = stringResource(R.string.code_length),
            error = state.lengthError,
            inputType = InputType.NUMBER,
            maxLength = 2,
            isPasswordToggleEnabled = false,
            onValueChange = { newLength ->
                onLengthChanged.invoke(newLength)
            },
            modifier = Modifier
                .padding(
                    start = dimensionResource(R.dimen.half_margin)
                )
                .weight(weight = 0.5f)
        )
    }
}

@Composable
private fun UrlTabContent(
    state: UrlTabState,
    onUrlChanged: (url: String) -> Unit
) {
    AppTextField(
        value = state.url,
        label = stringResource(R.string.url_cap),
        error = state.urlError,
        inputType = InputType.TEXT,
        onValueChange = { newUrl -> onUrlChanged.invoke(newUrl) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.element_margin),
                end = dimensionResource(R.dimen.element_margin),
                top = dimensionResource(R.dimen.element_margin)
            )
    )
}

@Preview
@Composable
fun LightTotpPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SetupOneTimePasswordScreen(
            state = newCustomTotpState(),
            periodProgressProvider = { 0.75f },
            onTabSelected = {},
            onTypeChanged = {},
            onSecretChanged = {},
            onAlgorithmChanged = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {},
            onUrlChanged = {}
        )
    }
}

@Preview
@Composable
fun LightHotpPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SetupOneTimePasswordScreen(
            state = newCustomHotpState(),
            periodProgressProvider = { 0.75f },
            onTabSelected = {},
            onTypeChanged = {},
            onSecretChanged = {},
            onAlgorithmChanged = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {},
            onUrlChanged = {}
        )
    }
}

@Preview
@Composable
fun LightUrlPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SetupOneTimePasswordScreen(
            state = newUrlTotpState(),
            periodProgressProvider = { 0.75f },
            onTabSelected = {},
            onTypeChanged = {},
            onSecretChanged = {},
            onAlgorithmChanged = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {},
            onUrlChanged = {}
        )
    }
}

@Preview
@Composable
fun DarkPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        SetupOneTimePasswordScreen(
            state = newCustomTotpState(),
            periodProgressProvider = { 0.75f },
            onTabSelected = {},
            onTypeChanged = {},
            onSecretChanged = {},
            onAlgorithmChanged = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {},
            onUrlChanged = {}
        )
    }
}

private fun newCustomTotpState() = SetupOneTimePasswordState(
    selectedTab = SetupOneTimePasswordTab.CUSTOM,
    code = "--- ---",
    isPeriodProgressVisible = true,
    customTabState = newTotpState(),
    urlTabState = UrlTabState(
        url = "",
        urlError = null
    )
)

private fun newCustomHotpState() = SetupOneTimePasswordState(
    selectedTab = SetupOneTimePasswordTab.CUSTOM,
    code = "--- ---",
    isPeriodProgressVisible = false,
    customTabState = newHotpState(),
    urlTabState = UrlTabState(
        url = "",
        urlError = null
    )
)

private fun newUrlTotpState() = SetupOneTimePasswordState(
    selectedTab = SetupOneTimePasswordTab.URL,
    code = "--- ---",
    isPeriodProgressVisible = true,
    customTabState = newHotpState(),
    urlTabState = UrlTabState(
        url = "otpauth://totp/Name:Issuer?secret=AAAABBBB&period=30&digits=6",
        urlError = null
    )
)

private fun newTotpState() = CustomTabState(
    types = listOf("TOTP", "HOTP"),
    selectedType = "TOTP",
    secret = "secret",
    isSecretVisible = false,
    algorithms = listOf("SHA1", "SHA256", "SHA512"),
    selectedAlgorithm = "SHA1",
    period = "30",
    counter = "",
    length = "6",
    secretError = null,
    periodError = null,
    counterError = null,
    lengthError = null,
    isPeriodVisible = true,
    isCounterVisible = false
)

private fun newHotpState() = CustomTabState(
    types = listOf("TOTP", "HOTP"),
    selectedType = "TOTP",
    secret = "secret",
    isSecretVisible = false,
    algorithms = listOf("SHA1", "SHA256", "SHA512"),
    selectedAlgorithm = "SHA1",
    period = "",
    counter = "123",
    length = "6",
    secretError = null,
    periodError = null,
    counterError = null,
    lengthError = null,
    isPeriodVisible = false,
    isCounterVisible = true
)