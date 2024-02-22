package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import com.ivanovsky.passnotes.domain.otp.OtpParametersValidator

class SetupOneTimePasswordInteractor {

    fun isPeriodValid(period: Int?): Boolean {
        return OtpParametersValidator.isPeriodValid(period)
    }

    fun isCounterValid(counter: Long?): Boolean {
        return OtpParametersValidator.isCounterValid(counter)
    }

    fun isDigitsValid(digits: Int?): Boolean {
        return OtpParametersValidator.isDigitsValid(digits)
    }

    fun isSecretValid(secret: String?): Boolean {
        return OtpParametersValidator.isSecretValid(secret)
    }
}