package com.ivanovsky.passnotes.domain.otp

import android.net.Uri
import com.ivanovsky.passnotes.domain.otp.OtpParametersValidator.isCounterValid
import com.ivanovsky.passnotes.domain.otp.OtpParametersValidator.isDigitsValid
import com.ivanovsky.passnotes.domain.otp.OtpParametersValidator.isPeriodValid
import com.ivanovsky.passnotes.domain.otp.OtpParametersValidator.isSecretValid
import com.ivanovsky.passnotes.domain.otp.model.HashAlgorithmType
import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import com.ivanovsky.passnotes.domain.otp.model.OtpTokenType
import com.ivanovsky.passnotes.util.StringUtils.AMPERSAND
import com.ivanovsky.passnotes.util.StringUtils.COLON
import com.ivanovsky.passnotes.util.StringUtils.COLON_URL_ENCODED
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.StringUtils.EQUALS
import com.ivanovsky.passnotes.util.StringUtils.QUESTION_MARK
import com.ivanovsky.passnotes.util.StringUtils.SLASH
import com.ivanovsky.passnotes.util.toIntSafely
import com.ivanovsky.passnotes.util.toLongSafely

object OtpUriFactory {

    private const val SCHEME = "otpauth"

    private const val URL_PARAM_ISSUER = "issuer"
    private const val URL_PARAM_SECRET = "secret"
    private const val URL_PARAM_DIGITS = "digits"
    private const val URL_PARAM_PERIOD = "period"
    private const val URL_PARAM_COUNTER = "counter"
    private const val URL_PARAM_ALGORITHM = "algorithm"

    fun createUri(token: OtpToken): String {
        return StringBuilder()
            .apply {
                append(SCHEME).append("://").append(token.type.rfcName)

                if (token.name.isNotEmpty() && token.issuer.isNotEmpty()) {
                    append(SLASH)
                    append(token.issuer).append(COLON_URL_ENCODED).append(token.name)
                } else if (token.name.isNotEmpty()) {
                    append(SLASH)
                    append(token.name)
                }

                append(QUESTION_MARK)

                val parameters = mutableMapOf(
                    URL_PARAM_SECRET to token.secret,
                    URL_PARAM_COUNTER to token.counter?.toString(),
                    URL_PARAM_DIGITS to token.digits.toString(),
                    URL_PARAM_PERIOD to token.periodInSeconds?.toString(),
                    URL_PARAM_ALGORITHM to token.algorithm.rfcName,
                    URL_PARAM_ISSUER to token.issuer
                )

                for ((key, value) in parameters) {
                    if (value.isNullOrEmpty()) {
                        continue
                    }

                    if (get(lastIndex) != QUESTION_MARK) {
                        append(AMPERSAND)
                    }

                    append(key).append(EQUALS).append(value)
                }
            }
            .toString()
    }

    fun parseUri(otpUri: String): OtpToken? {
        if (otpUri.isBlank()) {
            return null
        }

        var issuer: String?
        var secret: String? = null
        var digits: Int? = null
        var counter: Long? = null
        var period: Int? = null
        var algorithm: HashAlgorithmType? = null

        val uri = Uri.parse(otpUri.trim())
        if (uri.scheme?.lowercase() != SCHEME) {
            return null
        }

        val type = OtpTokenType.fromString(uri.authority?.trim() ?: EMPTY)

        val (name, parsedIssuer) = parsePath(uri.path)
        issuer = parsedIssuer

        val issuerParam = uri.getQueryParameter(URL_PARAM_ISSUER)?.trim()
        if (!issuerParam.isNullOrEmpty()) {
            issuer = issuerParam
        }

        val secretParam = uri.getQueryParameter(URL_PARAM_SECRET)?.trim()
        if (!secretParam.isNullOrEmpty()) {
            secret = secretParam
        }

        val digitsParam = uri.getQueryParameter(URL_PARAM_DIGITS)?.trim()
        if (!digitsParam.isNullOrEmpty()) {
            digits = digitsParam.toIntSafely()
        }

        val counterParam = uri.getQueryParameter(URL_PARAM_COUNTER)?.trim()
        if (!counterParam.isNullOrEmpty()) {
            counter = counterParam.toLongSafely()
        }

        val periodParam = uri.getQueryParameter(URL_PARAM_PERIOD)?.trim()
        if (!periodParam.isNullOrEmpty()) {
            period = periodParam.toIntSafely()
        }

        val algorithmParam = uri.getQueryParameter(URL_PARAM_ALGORITHM)?.trim()
        if (!algorithmParam.isNullOrEmpty()) {
            algorithm = HashAlgorithmType.fromString(algorithmParam)
        }

        val isValidTotpToken = isValidTotpParams(
            type = type,
            secret = secret,
            digits = digits,
            period = period
        )

        val isValidHotpToken = isValidHotpTokenParams(
            type = type,
            counter = counter,
            secret = secret,
            digits = digits
        )

        return when {
            isValidTotpToken -> OtpToken(
                type = OtpTokenType.TOTP,
                name = name ?: EMPTY,
                issuer = issuer ?: EMPTY,
                secret = secret ?: EMPTY,
                algorithm = algorithm ?: OtpToken.DEFAULT_HASH_ALGORITHM,
                digits = digits ?: OtpToken.DEFAULT_DIGITS,
                counter = null,
                periodInSeconds = period ?: OtpToken.DEFAULT_PERIOD_IN_SECONDS
            )

            isValidHotpToken -> OtpToken(
                type = OtpTokenType.HOTP,
                name = name ?: EMPTY,
                issuer = issuer ?: EMPTY,
                secret = secret ?: EMPTY,
                algorithm = algorithm ?: OtpToken.DEFAULT_HASH_ALGORITHM,
                digits = digits ?: OtpToken.DEFAULT_DIGITS,
                counter = counter,
                periodInSeconds = null
            )

            else -> null
        }
    }

    private fun isValidTotpParams(
        type: OtpTokenType?,
        secret: String?,
        digits: Int?,
        period: Int?
    ): Boolean {
        return type == OtpTokenType.TOTP &&
            isSecretValid(secret) &&
            (digits == null || isDigitsValid(digits)) &&
            (period == null || isPeriodValid(period))
    }

    private fun isValidHotpTokenParams(
        type: OtpTokenType?,
        secret: String?,
        counter: Long?,
        digits: Int?
    ): Boolean {
        return type == OtpTokenType.HOTP &&
            isSecretValid(secret) &&
            isCounterValid(counter) &&
            (digits == null || digits in OtpToken.DIGITS_RANGE)
    }

    private fun parsePath(path: String?): Pair<String?, String?> {
        if (path.isNullOrEmpty() || !path.startsWith(SLASH)) {
            return (null to null)
        }

        val cleanPath = path.substring(1).trim()
        if (cleanPath.isEmpty()) {
            return (null to null)
        }

        val values = cleanPath.split(COLON.toString(), COLON_URL_ENCODED)
            .map { value -> value.trim() }
            .filter { value -> value.isNotEmpty() }

        return when {
            values.size > 1 -> (values[1] to values[0])
            values.size == 1 -> (values.first() to null)
            else -> (null to null)
        }
    }
}