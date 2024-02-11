package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.domain.otp.model.OtpToken

interface OtpGenerator {
    val token: OtpToken
    fun generateCode(): String
}