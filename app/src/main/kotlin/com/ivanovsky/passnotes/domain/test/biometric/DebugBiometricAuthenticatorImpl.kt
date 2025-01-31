package com.ivanovsky.passnotes.domain.test.biometric

import androidx.fragment.app.FragmentActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDataCipher
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricAuthenticator
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog
import timber.log.Timber

class DebugBiometricAuthenticatorImpl(
    private val resourceProvider: ResourceProvider
) : BiometricAuthenticator {

    override fun authenticateForUnlock(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (decoder: BiometricDecoder) -> Unit
    ) {
        val dialog = ConfirmationDialog.newInstance(
            message = resourceProvider.getString(R.string.fake_biometric_auth_message),
            positiveButtonText = resourceProvider.getString(R.string.yes),
            negativeButtonText = resourceProvider.getString(R.string.no)
        )
            .apply {
                onConfirmed = {
                    Timber.d("Biometric authentication approved")
                    onSuccess.invoke(ClearTextBiometricDecoder())
                }
                onDenied = {
                    Timber.d("Biometric authentication denied")
                }
            }

        dialog.show(activity.supportFragmentManager, ConfirmationDialog.TAG)
    }

    override fun authenticateForSetup(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (encoder: BiometricEncoder) -> Unit
    ) {
        val dialog = ConfirmationDialog.newInstance(
            message = resourceProvider.getString(R.string.fake_biometric_setup_message),
            positiveButtonText = resourceProvider.getString(R.string.yes),
            negativeButtonText = resourceProvider.getString(R.string.no)
        )
            .apply {
                onConfirmed = {
                    Timber.d("Biometric setup approved")
                    onSuccess.invoke(ClearTextBiometricEncoder())
                }
                onDenied = {
                    Timber.d("Biometric setup denied")
                }
            }

        dialog.show(activity.supportFragmentManager, ConfirmationDialog.TAG)
    }
}