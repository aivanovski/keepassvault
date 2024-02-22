package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.otp.model.HashAlgorithmType
import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import com.ivanovsky.passnotes.domain.otp.model.OtpToken.Companion.DEFAULT_COUNTER
import com.ivanovsky.passnotes.domain.otp.model.OtpToken.Companion.DEFAULT_DIGITS
import com.ivanovsky.passnotes.domain.otp.model.OtpToken.Companion.DEFAULT_PERIOD_IN_SECONDS
import com.ivanovsky.passnotes.domain.otp.model.OtpTokenType
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.SetupOneTimePasswordScreen
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.SetupOneTimePasswordState
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toIntSafely
import com.ivanovsky.passnotes.util.toLongSafely
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.parameter.parametersOf

class SetupOneTimePasswordViewModel(
    private val interactor: SetupOneTimePasswordInteractor,
    private val router: Router,
    private val resourceProvider: ResourceProvider,
    themeProvider: ThemeProvider,
    private val args: SetupOneTimePasswordArgs
) : ViewModel() {

    private var type = OtpTokenType.TOTP
    private var secret = EMPTY
    private var isSecretVisible = false
    private var algorithm = OtpToken.DEFAULT_HASH_ALGORITHM
    private var period = DEFAULT_PERIOD_IN_SECONDS.toString()
    private var counter = DEFAULT_COUNTER.toString()
    private var length = DEFAULT_DIGITS.toString()
    private var secretError: String? = null
    private var periodError: String? = null
    private var counterError: String? = null
    private var lengthError: String? = null

    val theme = themeFlow(themeProvider)
    val state = MutableStateFlow(buildState())

    fun onSecretChanged(newSecret: String) {
        secret = newSecret
        secretError = null
        updateState()
    }

    fun onTypeChanged(newType: String) {
        type = OtpTokenType.fromString(newType) ?: OtpTokenType.TOTP
        updateState()
    }

    fun onPeriodChanged(newPeriod: String) {
        period = newPeriod
        periodError = validatePeriod(newPeriod)
        updateState()
    }

    fun onCounterChanged(newCounter: String) {
        counter = newCounter
        counterError = validateCounter(newCounter)
        updateState()
    }

    fun onLengthChanged(newLength: String) {
        length = newLength
        lengthError = validateLength(newLength)
        updateState()
    }

    fun onAlgorithmSelected(newAlgorithm: String) {
        algorithm = HashAlgorithmType.fromString(newAlgorithm) ?: OtpToken.DEFAULT_HASH_ALGORITHM
        updateState()
    }

    fun onSecretVisibilityChanged() {
        isSecretVisible = !isSecretVisible
        updateState()
    }

    fun onDoneClicked() {
        secretError = validateSecret(secret)
        updateState()

        if (hasError()) {
            return
        }

        val token = buildToken()

        router.exit()
        if (token != null) {
            router.sendResult(SetupOneTimePasswordScreen.RESULT_KEY, token)
        }
    }

    fun navigateBack() {
        router.exit()
    }

    private fun validatePeriod(period: String): String? {
        return if (!interactor.isPeriodValid(period.toIntSafely())) {
            resourceProvider.getString(R.string.generation_interval_invalid_value_message)
        } else {
            null
        }
    }

    private fun validateCounter(counter: String): String? {
        return if (!interactor.isCounterValid(counter.toLongSafely())) {
            resourceProvider.getString(R.string.counter_invalid_value_message)
        } else {
            null
        }
    }

    private fun validateLength(length: String): String? {
        return if (!interactor.isDigitsValid(length.toIntSafely())) {
            resourceProvider.getString(R.string.code_length_invalid_value_message)
        } else {
            null
        }
    }

    private fun validateSecret(secret: String): String? {
        return if (!interactor.isSecretValid(secret)) {
            resourceProvider.getString(R.string.empty_value_message)
        } else {
            null
        }
    }

    private fun hasError(): Boolean {
        return secretError != null ||
            periodError != null ||
            counterError != null ||
            lengthError != null
    }

    private fun buildToken(): OtpToken? {
        if (hasError()) {
            return null
        }

        return when (type) {
            OtpTokenType.TOTP -> {
                OtpToken(
                    type = OtpTokenType.TOTP,
                    name = args.tokenName ?: EMPTY,
                    issuer = args.tokenIssuer ?: EMPTY,
                    secret = secret.replace(" ", ""),
                    algorithm = algorithm,
                    digits = length.toIntSafely() ?: DEFAULT_DIGITS,
                    counter = null,
                    periodInSeconds = period.toIntSafely() ?: DEFAULT_PERIOD_IN_SECONDS
                )
            }

            OtpTokenType.HOTP -> {
                OtpToken(
                    type = OtpTokenType.HOTP,
                    name = args.tokenName ?: EMPTY,
                    issuer = args.tokenIssuer ?: EMPTY,
                    secret = secret,
                    algorithm = algorithm,
                    digits = length.toIntSafely() ?: DEFAULT_DIGITS,
                    counter = counter.toLongSafely() ?: DEFAULT_COUNTER,
                    periodInSeconds = null
                )
            }
        }
    }

    private fun updateState() {
        state.value = buildState()
    }

    private fun buildState(): SetupOneTimePasswordState {
        return SetupOneTimePasswordState(
            secret = secret,
            secretError = secretError,
            isSecretVisible = isSecretVisible,
            types = createTypeNames(OtpTokenType.entries),
            selectedType = type.toReadableString(),
            algorithms = createAlgorithmNames(HashAlgorithmType.entries),
            selectedAlgorithm = algorithm.toReadableString(),
            period = period,
            periodError = periodError,
            counter = counter,
            counterError = counterError,
            length = length,
            lengthError = lengthError,
            isPeriodVisible = (type == OtpTokenType.TOTP),
            isCounterVisible = (type == OtpTokenType.HOTP)
        )
    }

    private fun createTypeNames(types: List<OtpTokenType>): List<String> {
        return types.map { type -> type.toReadableString() }
    }

    private fun createAlgorithmNames(algorithms: List<HashAlgorithmType>): List<String> {
        return algorithms.map { algorithm -> algorithm.toReadableString() }
    }

    private fun OtpTokenType.toReadableString(): String = name

    private fun HashAlgorithmType.toReadableString(): String = name

    class Factory(
        private val args: SetupOneTimePasswordArgs
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<SetupOneTimePasswordViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}