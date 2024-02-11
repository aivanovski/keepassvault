package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import dev.robinohs.totpkt.otp.hotp.HotpGenerator as ExternalHotpGenerator

class HotpGenerator(
    override val token: OtpToken
) : OtpGenerator {

    private val generator = ExternalHotpGenerator(
        algorithm = token.algorithm.toExternalAlgorithm(),
        codeLength = token.digits,
    )

    override fun generateCode(): String {
        return generator.generateCode(token.secret.toByteArray(), token.counter ?: 0)
    }
}