package com.ivanovsky.passnotes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.*

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
}