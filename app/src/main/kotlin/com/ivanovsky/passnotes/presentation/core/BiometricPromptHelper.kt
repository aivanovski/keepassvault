package com.ivanovsky.passnotes.presentation.core

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDataCipher
import timber.log.Timber
import javax.crypto.Cipher

object BiometricPromptHelper {

    fun authenticateForUnlock(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (result: BiometricPrompt.AuthenticationResult) -> Unit,
    ) {
        val promptInfo = createPromptInfo(
            title = activity.getString(R.string.unlock_with_fingerprint),
            negativeButtonText = activity.getString(R.string.cancel)
        )
        authenticate(activity, cipher.getCipher(), promptInfo, onSuccess)
    }

    fun authenticateForSetup(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (result: BiometricPrompt.AuthenticationResult) -> Unit,
    ) {
        val promptInfo = createPromptInfo(
            title = activity.getString(R.string.setup_fingerprint_unlock),
            message = activity.getString(R.string.setup_fingerprint_unlock_message),
            negativeButtonText = activity.getString(R.string.cancel)
        )
        authenticate(activity, cipher.getCipher(), promptInfo, onSuccess)
    }

    private fun authenticate(
        activity: FragmentActivity,
        cipher: Cipher,
        info: BiometricPrompt.PromptInfo,
        onSuccess: (result: BiometricPrompt.AuthenticationResult) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                Timber.d("onAuthenticationError: errCode=%s, errString=%s", errCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.d("onAuthenticationFailed")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private fun createPromptInfo(
        title: String,
        message: String? = null,
        negativeButtonText: String
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .apply {
                message?.let { setDescription(message) }
            }
            .setTitle(title)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .build()
    }
}