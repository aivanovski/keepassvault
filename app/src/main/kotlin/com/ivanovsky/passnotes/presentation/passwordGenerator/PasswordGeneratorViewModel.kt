package com.ivanovsky.passnotes.presentation.passwordGenerator

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PasswordResource
import com.ivanovsky.passnotes.domain.interactor.passwordGenerator.PasswordGeneratorInteractor
import com.ivanovsky.passnotes.presentation.Screens.PasswordGeneratorScreen
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toIntSafely
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordGeneratorViewModel(
    private val interactor: PasswordGeneratorInteractor,
    lockInteractor: DatabaseLockInteractor,
    private val settings: Settings,
    private val dispatchers: DispatcherProvider,
    private val resourceProvider: ResourceProvider,
    observerBus: ObserverBus,
    private val router: Router
) : ViewModel() {

    val sliderMin = SLIDER_RANGE.first.toFloat()
    val sliderMax = SLIDER_RANGE.last.toFloat()
    val sliderValue = MutableLiveData(settings.passwordGeneratorSettings.length.toFloat())
    val password = MutableLiveData(EMPTY)
    val error = MutableLiveData<String>(null)
    val length = MutableLiveData(settings.passwordGeneratorSettings.length.toString())
    val isUppercaseChecked = MutableLiveData(true)
    val isLowercaseChecked = MutableLiveData(true)
    val isDigitsChecked = MutableLiveData(true)
    val isMinusChecked = MutableLiveData(false)
    val isUnderscoreChecked = MutableLiveData(true)
    val isSpaceChecked = MutableLiveData(false)
    val isSpecialChecked = MutableLiveData(false)
    val isBracketsChecked = MutableLiveData(false)
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    init {
        val generatorSettings = settings.passwordGeneratorSettings
        isUppercaseChecked.value = generatorSettings.isUpperCaseLettersEnabled
        isLowercaseChecked.value = generatorSettings.isLowerCaseLettersEnabled
        isDigitsChecked.value = generatorSettings.isDigitsEnabled
        isMinusChecked.value = generatorSettings.isMinusEnabled
        isUnderscoreChecked.value = generatorSettings.isUnderscoreEnabled
        isSpaceChecked.value = generatorSettings.isSpaceEnabled
        isSpecialChecked.value = generatorSettings.isSpecialEnabled
        isBracketsChecked.value = generatorSettings.isBracketsEnabled
    }

    private var lastSelectedResources: List<PasswordResource>? = null

    fun start() {
        onGenerateButtonClicked()
    }

    fun onGenerateButtonClicked() {
        val length = getLength()
        if (length != null) {
            invalidatePassword(length)
        }
    }

    fun onSymbolsCheckedChanged(isChecked: Boolean, resource: PasswordResource) {
        val resources = when (resource) {
            PasswordResource.UPPERCASE -> getSelectedPasswordResources(isUppercase = isChecked)
            PasswordResource.LOWERCASE -> getSelectedPasswordResources(isLowercase = isChecked)
            PasswordResource.DIGITS -> getSelectedPasswordResources(isDigits = isChecked)
            PasswordResource.MINUS -> getSelectedPasswordResources(isMinus = isChecked)
            PasswordResource.UNDERSCORE -> getSelectedPasswordResources(isUnderscore = isChecked)
            PasswordResource.SPACE -> getSelectedPasswordResources(isSpace = isChecked)
            PasswordResource.SPECIAL -> getSelectedPasswordResources(isSpecial = isChecked)
            PasswordResource.BRACKETS -> getSelectedPasswordResources(isBrackets = isChecked)
        }

        updateSettings(
            isEnabled = isChecked,
            resource = resource
        )

        error.value = if (isResourcesValid(resources)) {
            null
        } else {
            getErrorMessage(resources)
        }

        val length = getLength()
        if (lastSelectedResources != resources && length != null) {
            lastSelectedResources = resources
            invalidatePassword(length = length, resources = resources)
        }
    }

    fun onDoneButtonClicked() {
        val password = password.value?.toString() ?: return

        if (password.isNotEmpty()) {
            router.sendResult(PasswordGeneratorScreen.RESULT_KEY, password)
        }
        router.exit()
    }

    fun navigateBack() = router.exit()

    fun onLengthSliderPositionChanged(value: Int) {
        length.value = value.toString()

        onLengthChanged(value)
    }

    fun onLengthInputChanged(value: String) {
        val newLength = parseLength(value) ?: return

        val newSliderValue = if (newLength in SLIDER_RANGE) {
            newLength.toFloat()
        } else {
            sliderMax
        }

        if (sliderValue.value != newSliderValue) {
            sliderValue.value = newSliderValue
        }

        onLengthChanged(newLength)
    }

    private fun onLengthChanged(newLength: Int) {
        settings.passwordGeneratorSettings = settings.passwordGeneratorSettings.copy(
            length = newLength
        )

        invalidatePassword(length = newLength)
    }

    private fun updateSettings(
        isEnabled: Boolean,
        resource: PasswordResource
    ) {
        val currentSettings = settings.passwordGeneratorSettings

        val newSettings = when (resource) {
            PasswordResource.UPPERCASE -> currentSettings.copy(
                isUpperCaseLettersEnabled = isEnabled,
            )

            PasswordResource.LOWERCASE -> currentSettings.copy(
                isLowerCaseLettersEnabled = isEnabled
            )

            PasswordResource.DIGITS -> currentSettings.copy(
                isDigitsEnabled = isEnabled
            )

            PasswordResource.MINUS -> currentSettings.copy(
                isMinusEnabled = isEnabled
            )

            PasswordResource.UNDERSCORE -> currentSettings.copy(
                isUnderscoreEnabled = isEnabled
            )

            PasswordResource.SPACE -> currentSettings.copy(
                isSpaceEnabled = isEnabled
            )

            PasswordResource.SPECIAL -> currentSettings.copy(
                isSpecialEnabled = isEnabled
            )

            PasswordResource.BRACKETS -> currentSettings.copy(
                isBracketsEnabled = isEnabled
            )
        }

        settings.passwordGeneratorSettings = newSettings
    }

    private fun parseLength(length: String): Int? {
        return length.toIntSafely()
    }

    private fun isResourcesValid(resources: List<PasswordResource>): Boolean {
        return resources.size > 1 ||
            (resources.size == 1 && !resources.contains(PasswordResource.SPACE))
    }

    private fun getErrorMessage(resources: List<PasswordResource>): String? {
        return if (!isResourcesValid(resources)) {
            resourceProvider.getString(R.string.no_options_selected_message)
        } else {
            null
        }
    }

    private fun invalidatePassword(
        length: Int,
        resources: List<PasswordResource> = getSelectedPasswordResources()
    ) {
        viewModelScope.launch {
            password.value = withContext(dispatchers.Default) {
                interactor.generatePassword(length, resources)
            }
        }
    }

    private fun getLength(): Int? {
        val length = length.value?.toIntSafely() ?: return null

        return if (length in LENGTH_RANGE) {
            length
        } else {
            null
        }
    }

    private fun getSelectedPasswordResources(
        isUppercase: Boolean = isUppercaseChecked.value ?: false,
        isLowercase: Boolean = isLowercaseChecked.value ?: false,
        isDigits: Boolean = isDigitsChecked.value ?: false,
        isMinus: Boolean = isMinusChecked.value ?: false,
        isUnderscore: Boolean = isUnderscoreChecked.value ?: false,
        isSpace: Boolean = isSpaceChecked.value ?: false,
        isSpecial: Boolean = isSpecialChecked.value ?: false,
        isBrackets: Boolean = isBracketsChecked.value ?: false
    ): List<PasswordResource> {
        val resources = mutableListOf<PasswordResource>()

        if (isUppercase) {
            resources.add(PasswordResource.UPPERCASE)
        }
        if (isLowercase) {
            resources.add(PasswordResource.LOWERCASE)
        }
        if (isDigits) {
            resources.add(PasswordResource.DIGITS)
        }
        if (isMinus) {
            resources.add(PasswordResource.MINUS)
        }
        if (isUnderscore) {
            resources.add(PasswordResource.UNDERSCORE)
        }
        if (isSpace) {
            resources.add(PasswordResource.SPACE)
        }
        if (isSpecial) {
            resources.add(PasswordResource.SPECIAL)
        }
        if (isBrackets) {
            resources.add(PasswordResource.BRACKETS)
        }

        return resources
    }

    companion object {
        private val SLIDER_RANGE = 1..64
        private val LENGTH_RANGE = 1..999
    }
}