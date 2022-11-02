package com.ivanovsky.passnotes.domain.biometric

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDataCipher
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricDecoderImpl
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoderImpl
import timber.log.Timber
import javax.crypto.Cipher

class BiometricAuthenticatorImpl : BiometricAuthenticator {

    override fun authenticateForUnlock(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (decoder: BiometricDecoder) -> Unit
    ) {
        val promptInfo = createPromptInfo(
            title = activity.getString(R.string.unlock_with_fingerprint),
            negativeButtonText = activity.getString(R.string.cancel)
        )
        authenticate(activity, cipher.getCipher(), promptInfo) { result ->
            val decoderCipher = result.cryptoObject?.cipher
            if (decoderCipher != null) {
                onSuccess.invoke(BiometricDecoderImpl(decoderCipher))
            }
        }
    }

    override fun authenticateForSetup(
        activity: FragmentActivity,
        cipher: BiometricDataCipher,
        onSuccess: (encoder: BiometricEncoder) -> Unit
    ) {
        val promptInfo = createPromptInfo(
            title = activity.getString(R.string.setup_fingerprint_unlock),
            message = activity.getString(R.string.setup_fingerprint_unlock_message),
            negativeButtonText = activity.getString(R.string.cancel)
        )
        authenticate(activity, cipher.getCipher(), promptInfo) { result ->
            val encoderCipher = result.cryptoObject?.cipher
            if (encoderCipher != null) {
                onSuccess.invoke(BiometricEncoderImpl(encoderCipher))
            }
        }
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