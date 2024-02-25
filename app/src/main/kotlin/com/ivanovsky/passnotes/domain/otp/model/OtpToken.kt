package com.ivanovsky.passnotes.domain.otp.model

data class OtpToken(
    val type: OtpTokenType,
    val name: String,
    val issuer: String,
    val secret: String,
    val algorithm: HashAlgorithmType,
    val digits: Int,
    val periodInSeconds: Int?,
    val counter: Long?
) {

    companion object {
        val DEFAULT_HASH_ALGORITHM = HashAlgorithmType.SHA1

        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD_IN_SECONDS = 30
        const val DEFAULT_COUNTER = 1L

        val HOTP_COUNTER_RANGE = 0..Long.MAX_VALUE
        val TOTP_PERIOD_RANGE = 1..900
        val DIGITS_RANGE = 4..18
    }
}