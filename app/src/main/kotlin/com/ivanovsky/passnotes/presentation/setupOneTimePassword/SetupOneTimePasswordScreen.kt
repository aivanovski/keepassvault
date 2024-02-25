package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.compose.AppDropdownMenu
import com.ivanovsky.passnotes.presentation.core.compose.AppTextField
import com.ivanovsky.passnotes.presentation.core.compose.DarkTheme
import com.ivanovsky.passnotes.presentation.core.compose.LightTheme
import com.ivanovsky.passnotes.presentation.core.compose.ThemedScreenPreview
import com.ivanovsky.passnotes.presentation.core.compose.model.InputType
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.SetupOneTimePasswordState

@Composable
fun SetupOneTimePasswordScreen(
    viewModel: SetupOneTimePasswordViewModel
) {
    val state by viewModel.state.collectAsState()

    SetupOneTimePasswordScreen(
        state = state,
        onTypeSelected = viewModel::onTypeChanged,
        onSecretChanged = viewModel::onSecretChanged,
        onAlgorithmSelected = viewModel::onAlgorithmSelected,
        onPeriodChanged = viewModel::onPeriodChanged,
        onCounterChanged = viewModel::onCounterChanged,
        onLengthChanged = viewModel::onLengthChanged,
        onSecretVisibilityChanged = viewModel::onSecretVisibilityChanged
    )
}

@Composable
private fun SetupOneTimePasswordScreen(
    state: SetupOneTimePasswordState,
    onTypeSelected: (type: String) -> Unit,
    onAlgorithmSelected: (algorithm: String) -> Unit,
    onPeriodChanged: (period: String) -> Unit,
    onCounterChanged: (counter: String) -> Unit,
    onLengthChanged: (length: String) -> Unit,
    onSecretChanged: (secret: String) -> Unit,
    onSecretVisibilityChanged: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
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
                    top = dimensionResource(R.dimen.half_margin)
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
                onOptionSelected = { newType -> onTypeSelected.invoke(newType) },
                modifier = Modifier
                    .padding(end = dimensionResource(R.dimen.half_margin))
                    .weight(weight = 0.5f)
            )

            AppDropdownMenu(
                label = stringResource(R.string.algorithm),
                options = state.algorithms,
                selectedOption = state.selectedAlgorithm,
                onOptionSelected = { newAlgorithm -> onAlgorithmSelected.invoke(newAlgorithm) },
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
}

@Preview
@Composable
fun LightTotpPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SetupOneTimePasswordScreen(
            state = newTotpState(),
            onTypeSelected = {},
            onSecretChanged = {},
            onAlgorithmSelected = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {}
        )
    }
}

@Preview
@Composable
fun LightHotpPreview() {
    ThemedScreenPreview(theme = LightTheme) {
        SetupOneTimePasswordScreen(
            state = newHotpState(),
            onTypeSelected = {},
            onSecretChanged = {},
            onAlgorithmSelected = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {}
        )
    }
}

@Preview
@Composable
fun DarkPreview() {
    ThemedScreenPreview(theme = DarkTheme) {
        SetupOneTimePasswordScreen(
            state = newTotpState(),
            onTypeSelected = {},
            onSecretChanged = {},
            onAlgorithmSelected = {},
            onPeriodChanged = {},
            onCounterChanged = {},
            onLengthChanged = {},
            onSecretVisibilityChanged = {}
        )
    }
}

private fun newTotpState() = SetupOneTimePasswordState(
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

private fun newHotpState() = SetupOneTimePasswordState(
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