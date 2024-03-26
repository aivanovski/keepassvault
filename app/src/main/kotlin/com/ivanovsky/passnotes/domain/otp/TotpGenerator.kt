package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import dev.robinohs.totpkt.otp.totp.TotpGenerator as ExternalTotpGenerator
import dev.robinohs.totpkt.otp.totp.timesupport.calculateTimeslotBeginning
import dev.robinohs.totpkt.otp.totp.timesupport.generateCode
import java.time.Duration

class TotpGenerator(
    override val token: OtpToken
) : OtpGenerator {

    private val generator = ExternalTotpGenerator(
        algorithm = token.algorithm.toExternalAlgorithm(),
        codeLength = token.digits,
        timePeriod = Duration.ofSeconds((token.periodInSeconds ?: 0).toLong())
    )

    val periodInMillis = (token.periodInSeconds ?: 0) * 1000L

    override fun generateCode(): String {
        return try {
            generator.generateCode(token.secret.toByteArray())
        } catch (exception: Exception) {
            token.createCodePlaceholder()
        }
    }

    fun getNextTimeslotStart(): Long {
        return generator.calculateTimeslotBeginning() + periodInMillis
    }

    fun getRemainingTime(): Long {
        val nextTimeslotTime = generator.calculateTimeslotBeginning() + periodInMillis
        val remaining = nextTimeslotTime - System.currentTimeMillis()
        return if (remaining > 0) {
            remaining
        } else {
            0
        }
    }
}