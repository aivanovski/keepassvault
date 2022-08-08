package com.ivanovsky.passnotes.presentation.password_generator

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PasswordResource
import com.ivanovsky.passnotes.domain.interactor.password_generator.PasswordGeneratorInteractor
import com.ivanovsky.passnotes.presentation.Screens.PasswordGeneratorScreen
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toIntSafely
import kotlin.math.absoluteValue

class PasswordGeneratorViewModel(
    private val interactor: PasswordGeneratorInteractor,
    private val resourceProvider: ResourceProvider,
    private val router: Router
) : ViewModel() {

    val password = MutableLiveData(EMPTY)
    val error = MutableLiveData<String>(null)
    val length = MutableLiveData(DEFAULT_LENGTH.toString())
    val isUppercaseChecked = MutableLiveData(true)
    val isLowercaseChecked = MutableLiveData(true)
    val isDigitsChecked = MutableLiveData(true)
    val isMinusChecked = MutableLiveData(false)
    val isUnderscoreChecked = MutableLiveData(true)
    val isSpaceChecked = MutableLiveData(false)
    val isSpecialChecked = MutableLiveData(false)
    val isBracketsChecked = MutableLiveData(false)
    private var lastSelectedResources: List<PasswordResource>? = null

    fun start() {
        password.value = generatePassword(getSelectedPasswordResources())
    }

    fun onGenerateButtonClicked() {
        password.value = generatePassword(getSelectedPasswordResources())
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

        error.value = if (isResourcesValid(resources)) {
            null
        } else {
            getErrorMessage(resources)
        }

        if (lastSelectedResources != resources) {
            lastSelectedResources = resources
            password.value = generatePassword(resources)
        }
    }

    fun onLengthButtonClicked(value: Int) {
        length.value = value.toString()
        password.value = generatePassword(getSelectedPasswordResources())
    }

    fun onDoneButtonClicked() {
        val password = password.value?.toString() ?: return

        if (password.isNotEmpty()) {
            router.sendResult(PasswordGeneratorScreen.RESULT_KEY, password)
        }
        router.exit()
    }

    fun navigateBack() = router.exit()

    private fun isResourcesValid(resources: List<PasswordResource>): Boolean {
        return resources.size > 1 || (resources.size == 1 && !resources.contains(PasswordResource.SPACE))
    }

    private fun getErrorMessage(resources: List<PasswordResource>): String? {
        return if (!isResourcesValid(resources)) {
            resourceProvider.getString(R.string.no_options_selected_message)
        } else {
            null
        }
    }

    private fun generatePassword(resources: List<PasswordResource>): String {
        val length = length.value?.toIntSafely()?.absoluteValue ?: DEFAULT_LENGTH
        return interactor.generatePassword(length, resources)
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
            resources.add(PasswordResource.SPECIAL)
        }

        return resources
    }

    companion object {
        private const val DEFAULT_LENGTH = 12
    }
}