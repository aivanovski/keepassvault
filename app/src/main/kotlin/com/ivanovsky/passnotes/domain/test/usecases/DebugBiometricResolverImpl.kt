package com.ivanovsky.passnotes.domain.test.usecases

import android.content.Context
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.OnSettingsChangeListener
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractorImpl
import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.test.biometric.DebugBiometricInteractorImpl
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class DebugBiometricResolverImpl(
    private val settings: Settings,
    private val context: Context,
    private val resourceProvider: ResourceProvider
) : BiometricResolver {

    private val mutableHolder = MutableInteractorHolder(instantiateInteractor())
    private val settingsListener = createSettingsListener()

    init {
        settings.register(settingsListener)
    }

    override fun getInteractor(): BiometricInteractor = mutableHolder

    private fun createSettingsListener(): OnSettingsChangeListener =
        OnSettingsChangeListener { key ->
            if (key == SettingsImpl.Pref.TEST_TOGGLES) {
                reload()
            }
        }

    private fun reload() {
        mutableHolder.underlyingInteractor.value = instantiateInteractor()
    }

    private fun instantiateInteractor(): BiometricInteractor {
        val isFakeBiometricEnabled = settings.testToggles?.isFakeBiometricEnabled ?: false

        Timber.d("instantiateInteractor: isFakeBiometricsEnabled=%s", isFakeBiometricEnabled)

        return if (isFakeBiometricEnabled) {
            DebugBiometricInteractorImpl(resourceProvider)
        } else {
            BiometricInteractorImpl(context)
        }
    }

    private class MutableInteractorHolder(
        interactor: BiometricInteractor
    ) : BiometricInteractor {

        val underlyingInteractor = MutableStateFlow(interactor)

        override fun getAuthenticator(): BiometricAuthenticator =
            underlyingInteractor.value.getAuthenticator()

        override fun isBiometricUnlockAvailable(): Boolean =
            underlyingInteractor.value.isBiometricUnlockAvailable()

        override fun getCipherForEncryption(): OperationResult<BiometricEncoder> =
            underlyingInteractor.value.getCipherForEncryption()

        override fun getCipherForDecryption(
            biometricData: BiometricData
        ): OperationResult<BiometricDecoder> =
            underlyingInteractor.value.getCipherForDecryption(biometricData)

        override fun clearStoredData(): OperationResult<Boolean> =
            underlyingInteractor.value.clearStoredData()
    }
}