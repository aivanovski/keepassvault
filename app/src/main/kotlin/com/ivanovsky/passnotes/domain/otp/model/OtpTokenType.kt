package com.ivanovsky.passnotes.domain.otp.model

enum class OtpTokenType(val rfcName: String) {
    TOTP(rfcName = "totp"),
    HOTP(rfcName = "hotp");

    companion object {

        fun fromString(value: String): OtpTokenType? {
            return entries.firstOrNull { type -> value == type.rfcName }
        }
    }
}