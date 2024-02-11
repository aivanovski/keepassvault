package com.ivanovsky.passnotes.domain.otp

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.RobolectricApp
import com.ivanovsky.passnotes.domain.otp.model.HashAlgorithmType
import com.ivanovsky.passnotes.domain.otp.model.HashAlgorithmType.SHA1
import com.ivanovsky.passnotes.domain.otp.model.OtpToken
import com.ivanovsky.passnotes.domain.otp.model.OtpTokenType
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.StringUtils.LINE_BREAK
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricApp::class, sdk = [29])
class OtpUriFactoryTest {

    @Test
    fun `parseUri should parse valid uri's`() {
        listOf(
            Pair(
                """
                    otpauth://totp/$ISSUER:$NAME?secret=$SECRET&digits=$DIGITS
                    &period=$PERIOD&algorithm=$SHA1
                """.clearUri(),
                newTotpToken(
                    name = NAME,
                    issuer = ISSUER,
                    secret = SECRET,
                    algorithm = SHA1,
                    digits = DIGITS,
                    periodInSeconds = PERIOD
                )
            ),

            Pair(
                """
                    otpauth://hotp/$ISSUER:$NAME?secret=$SECRET&digits=$DIGITS
                    &algorithm=$SHA1&counter=$COUNTER
                """.clearUri(),
                newHotpToken(
                    name = NAME,
                    issuer = ISSUER,
                    secret = SECRET,
                    algorithm = SHA1,
                    counter = COUNTER,
                    digits = DIGITS
                )
            )
        )
            .forEach { (uri, expected) ->
                val result = OtpUriFactory.parseUri(uri)
                assertThat(result).isEqualTo(expected)
            }
    }

    @Test
    fun `parseUri should set default values`() {
        listOf(
            Pair(
                "otpauth://totp/$NAME?secret=$SECRET",
                newTotpToken(
                    name = NAME,
                    secret = SECRET
                )
            ),

            Pair(
                "otpauth://hotp/$NAME?secret=$SECRET&counter=$COUNTER",
                newHotpToken(
                    name = NAME,
                    secret = SECRET,
                    counter = COUNTER
                )
            )
        )
            .forEach { (uri, expected) ->
                val result = OtpUriFactory.parseUri(uri)
                assertThat(result).isEqualTo(expected)
            }
    }

    @Test
    fun `parseUir should override issuer from parameter`() {
        listOf(
            Pair(
                "otpauth://totp/$ISSUER1:$NAME?secret=$SECRET&issuer=$ISSUER2",
                newTotpToken(
                    name = NAME,
                    secret = SECRET,
                    issuer = ISSUER2
                )
            ),

            Pair(
                "otpauth://totp/$NAME?secret=$SECRET&issuer=$ISSUER",
                newTotpToken(
                    name = NAME,
                    secret = SECRET,
                    issuer = ISSUER
                )
            ),
        )
            .forEach { (uri, expected) ->
                val result = OtpUriFactory.parseUri(uri)
                assertThat(result).isEqualTo(expected)
            }
    }

    @Test
    fun `parseUri should ignore invalid values`() {
        listOf(
            Pair(
                "otpauth://totp/$NAME?secret=$SECRET&counter=$COUNTER",
                newTotpToken(
                    name = NAME,
                    secret = SECRET
                )
            ),

            Pair(
                "otpauth://hotp/$NAME?secret=$SECRET&counter=$COUNTER&period=$PERIOD",
                newHotpToken(
                    name = NAME,
                    secret = SECRET,
                    counter = COUNTER
                )
            )
        )
            .forEach { (uri, expected) ->
                val result = OtpUriFactory.parseUri(uri)
                assertThat(result).isEqualTo(expected)
            }
    }

    @Test
    fun `parseUir should return null`() {
        listOf(
            "",
            "http://example.com", // not a TOTP/HOTP uri

            "otpauth://totp/$NAME?period=$PERIOD", // no secret
            "otpauth://hotp/$NAME?counter=$COUNTER", // no secret
            "otpauth://hotp/$NAME?secret=$SECRET", // no counter

            "otpauth://totp/$NAME?secret=$SECRET&period=$INVALID_PERIOD", // invalid period value
            "otpauth://totp/$NAME?secret=$SECRET&digits=$INVALID_DIGITS", // invalid digits value
        )
            .forEach { uri ->
                val result = OtpUriFactory.parseUri(uri)
                assertThat(result).isNull()
            }
    }

    private fun newTotpToken(
        name: String = EMPTY,
        issuer: String = EMPTY,
        secret: String,
        algorithm: HashAlgorithmType = OtpToken.DEFAULT_HASH_ALGORITHM,
        digits: Int = OtpToken.DEFAULT_DIGITS,
        periodInSeconds: Int = OtpToken.DEFAULT_PERIOD_IN_SECONDS
    ): OtpToken =
        OtpToken(
            type = OtpTokenType.TOTP,
            name = name,
            issuer = issuer,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            periodInSeconds = periodInSeconds,
            counter = null
        )

    private fun newHotpToken(
        name: String = EMPTY,
        issuer: String = EMPTY,
        secret: String,
        algorithm: HashAlgorithmType = OtpToken.DEFAULT_HASH_ALGORITHM,
        counter: Long,
        digits: Int = OtpToken.DEFAULT_DIGITS
    ): OtpToken =
        OtpToken(
            type = OtpTokenType.HOTP,
            name = name,
            issuer = issuer,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            periodInSeconds = null,
            counter = counter
        )

    private fun String.clearUri(): String {
        return this
            .trimIndent()
            .replace(LINE_BREAK, EMPTY)
    }

    companion object {
        private const val SECRET = "ABCDEFGHIJKLMN123456"
        private const val ISSUER = "Issuer"
        private const val ISSUER1 = "Issuer1"
        private const val ISSUER2 = "Issuer2"
        private const val NAME = "Name"
        private const val DIGITS = 12
        private const val PERIOD = 123
        private const val COUNTER = 789L
        private const val INVALID_DIGITS = 1000
        private const val INVALID_PERIOD = 1000
    }
}