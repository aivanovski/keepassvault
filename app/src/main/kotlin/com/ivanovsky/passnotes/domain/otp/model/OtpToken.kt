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
    }
}