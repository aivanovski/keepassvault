package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.domain.otp.model.OtpToken

object OtpParametersValidator {

    fun isPeriodValid(period: Int?): Boolean {
        return (period != null && period in OtpToken.TOTP_PERIOD_RANGE)
    }

    fun isCounterValid(counter: Long?): Boolean {
        return (counter != null && counter in OtpToken.HOTP_COUNTER_RANGE)
    }

    fun isDigitsValid(digits: Int?): Boolean {
        return (digits != null && digits in OtpToken.DIGITS_RANGE)
    }

    fun isSecretValid(secret: String?): Boolean {
        return secret != null && secret.trim().isNotEmpty()
    }
}