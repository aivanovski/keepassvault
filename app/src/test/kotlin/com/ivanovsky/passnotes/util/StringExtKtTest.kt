package com.ivanovsky.passnotes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.UUID

class StringExtKtTest {

    @Test
    fun `toUUID should return UUID`() {
        val uid = UUID.randomUUID()

        val uidStr = uid.toString()
        val uidCleanStr = uid.toCleanString()

        assertThat(uidStr.toUUID()).isEqualTo(uid)
        assertThat(uidCleanStr.toUUID()).isEqualTo(uid)
    }

    @Test
    fun `toUUID should not crash`() {
        val malformedUid = "5561a431-69c3-410c-a437-02cbe06ea75a".replace("a", "")

        assertThat("".toUUID()).isEqualTo(null)
        assertThat("ffff".toUUID()).isEqualTo(null)
        assertThat(malformedUid.toUUID()).isEqualTo(null)
    }

    @Test
    fun `substituteAt should work`() {
        assertThat("0123456789".substituteAt(0, 10, '*')).isEqualTo(
            (0..9).map { '*' }.joinToString(separator = "")
        )
        assertThat("".substituteAt(0, 0, '*')).isEqualTo("")
        assertThat("0123456789".substituteAt(0, 4, '*')).isEqualTo("****456789")
        assertThat("0123456789".substituteAt(5, 10, '*')).isEqualTo("01234*****")
        assertThat("0123456789".substituteAt(2, 6, '*')).isEqualTo("01****6789")
    }
}