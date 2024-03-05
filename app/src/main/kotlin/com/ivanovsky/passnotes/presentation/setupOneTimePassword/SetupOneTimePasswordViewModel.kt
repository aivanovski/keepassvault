package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.otp.HotpGenerator
import com.ivanovsky.passnotes.domain.otp.OtpCodeFormatter
import com.ivanovsky.passnotes.domain.otp.OtpFlowFactory
import com.ivanovsky.passnotes.domain.otp.OtpGenerator
import com.ivanovsky.passnotes.domain.otp.OtpUriFactory
import com.ivanovsky.passnotes.domain.otp.TotpGenerator
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
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.CustomTabState
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.SetupOneTimePasswordState
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.SetupOneTimePasswordTab
import com.ivanovsky.passnotes.presentation.setupOneTimePassword.model.UrlTabState
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.removeSpaces
import com.ivanovsky.passnotes.util.toIntSafely
import com.ivanovsky.passnotes.util.toLongSafely
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf

class SetupOneTimePasswordViewModel(
    private val interactor: SetupOneTimePasswordInteractor,
    private val router: Router,
    private val resourceProvider: ResourceProvider,
    themeProvider: ThemeProvider,
    private val args: SetupOneTimePasswordArgs
) : ViewModel() {

    private var generator: OtpGenerator? = null
    private var selectedTab = SetupOneTimePasswordTab.CUSTOM
    private var url: String = EMPTY
    private var type = OtpTokenType.TOTP
    private var secret = EMPTY
    private var isSecretVisible = false
    private var algorithm = OtpToken.DEFAULT_HASH_ALGORITHM
    private var period = DEFAULT_PERIOD_IN_SECONDS.toString()
    private var counter = DEFAULT_COUNTER.toString()
    private var length = DEFAULT_DIGITS.toString()
    private var code = createCodePlaceholder()
    private var urlError: String? = null
    private var secretError: String? = null
    private var periodError: String? = null
    private var counterError: String? = null
    private var lengthError: String? = null

    val theme = themeFlow(themeProvider)

    private val token = MutableStateFlow<OtpToken?>(null)
    val state = MutableStateFlow(buildState())
    val periodProgress = buildTokenLifespanFlow()

    fun onSecretChanged(newSecret: String) {
        secret = newSecret
        secretError = null

        updateToken()
        updateCode()

        updateState()
    }

    fun onTypeChanged(newType: String) {
        type = OtpTokenType.fromString(newType) ?: OtpTokenType.TOTP

        updateToken()
        updateCode()

        updateState()
    }

    fun onPeriodChanged(newPeriod: String) {
        period = newPeriod
        periodError = validatePeriod(newPeriod)

        updateToken()
        updateCode()

        updateState()
    }

    fun onCounterChanged(newCounter: String) {
        counter = newCounter
        counterError = validateCounter(newCounter)

        updateToken()
        updateCode()

        updateState()
    }

    fun onLengthChanged(newLength: String) {
        length = newLength
        lengthError = validateLength(newLength)

        updateToken()
        updateCode()

        updateState()
    }

    fun onAlgorithmChanged(newAlgorithm: String) {
        algorithm = HashAlgorithmType.fromString(newAlgorithm) ?: OtpToken.DEFAULT_HASH_ALGORITHM

        updateToken()
        updateCode()

        updateState()
    }

    fun onUrlChanged(newUrl: String) {
        url = newUrl
        urlError = if (newUrl.isNotEmpty()) {
            validateUrl(newUrl)
        } else {
            null
        }

        updateToken()
        updateCode()

        updateState()
    }

    fun onSecretVisibilityChanged() {
        isSecretVisible = !isSecretVisible
        updateState()
    }

    fun onDoneClicked() {
        when (selectedTab) {
            SetupOneTimePasswordTab.CUSTOM -> {
                secretError = validateSecret(secret)
            }

            SetupOneTimePasswordTab.URL -> {
                urlError = validateUrl(url)
            }
        }

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

    fun onTabChanged(tab: SetupOneTimePasswordTab) {
        selectedTab = tab

        updateToken()
        updateCode()

        updateState()
    }

    fun navigateBack() {
        router.exit()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun buildTokenLifespanFlow(): Flow<Float> {
        return token
            .flatMapLatest { token ->
                if (token != null && token.type == OtpTokenType.TOTP) {
                    OtpFlowFactory.createLifespanFlow(TotpGenerator(token))
                        .map { progress -> progress / 100f }
                } else {
                    flowOf(0f)
                }
            }
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
        return when {
            secret.isBlank() -> {
                resourceProvider.getString(R.string.should_not_be_empty)
            }

            !interactor.isSecretValid(secret) -> {
                resourceProvider.getString(R.string.invalid_value)
            }

            else -> {
                null
            }
        }
    }

    private fun validateUrl(url: String): String? {
        return when {
            url.isBlank() -> {
                resourceProvider.getString(R.string.should_not_be_empty)
            }

            !interactor.isUrlValid(url) -> {
                resourceProvider.getString(R.string.invalid_value)
            }

            else -> null
        }
    }

    private fun hasError(): Boolean {
        return when (selectedTab) {
            SetupOneTimePasswordTab.CUSTOM -> hasCustomTabError()
            SetupOneTimePasswordTab.URL -> hasUrlTabError()
        }
    }

    private fun hasCustomTabError(): Boolean {
        return secretError != null ||
            periodError != null ||
            counterError != null ||
            lengthError != null
    }

    private fun hasUrlTabError(): Boolean {
        return urlError != null
    }

    private fun buildToken(): OtpToken? {
        return when (selectedTab) {
            SetupOneTimePasswordTab.CUSTOM -> buildTokenFromCustomParams()
            SetupOneTimePasswordTab.URL -> buildTokenFromUrl()
        }
    }

    private fun buildTokenFromCustomParams(): OtpToken? {
        val cleanedSecret = secret.removeSpaces()

        if (hasError() || !interactor.isSecretValid(cleanedSecret)) {
            return null
        }

        return when (type) {
            OtpTokenType.TOTP -> {
                OtpToken(
                    type = OtpTokenType.TOTP,
                    name = args.tokenName ?: EMPTY,
                    issuer = args.tokenIssuer ?: EMPTY,
                    secret = cleanedSecret,
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
                    secret = cleanedSecret,
                    algorithm = algorithm,
                    digits = length.toIntSafely() ?: DEFAULT_DIGITS,
                    counter = counter.toLongSafely() ?: DEFAULT_COUNTER,
                    periodInSeconds = null
                )
            }
        }
    }

    private fun buildTokenFromUrl(): OtpToken? {
        if (hasError()) {
            return null
        }

        return OtpUriFactory.parseUri(url)
    }

    private fun updateToken() {
        token.value = buildToken()
    }

    private fun updateCode() {
        val currentToken = token.value

        generator = when (currentToken?.type) {
            OtpTokenType.TOTP -> TotpGenerator(currentToken)
            OtpTokenType.HOTP -> HotpGenerator(currentToken)
            else -> null
        }

        code = generator?.let { generator ->
            OtpCodeFormatter.format(generator.generateCode())
        }
            ?: createCodePlaceholder()
    }

    private fun createCodePlaceholder(): String {
        val length = this.length.toIntSafely() ?: DEFAULT_DIGITS
        return OtpCodeFormatter.format("-".repeat(length))
    }

    private fun updateState() {
        state.value = buildState()
    }

    private fun buildState(): SetupOneTimePasswordState {
        val currentToken = token.value

        return SetupOneTimePasswordState(
            selectedTab = selectedTab,
            code = code,
            isPeriodProgressVisible = (currentToken != null &&
                currentToken.type == OtpTokenType.TOTP),
            customTabState = CustomTabState(
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
            ),
            urlTabState = UrlTabState(
                url = url,
                urlError = urlError
            )
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