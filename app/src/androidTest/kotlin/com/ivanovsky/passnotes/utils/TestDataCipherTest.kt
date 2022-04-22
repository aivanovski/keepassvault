package com.ivanovsky.passnotes.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.PLAIN_TEXT
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestDataCipherTest {

    @Test
    fun encodeAndDecodeShouldWork() {
        // arrange

        // act
        val encoded = TestDataCipher().encode(PLAIN_TEXT)
        requireNotNull(encoded)
        val decoded = TestDataCipher().decode(encoded)

        // assert
        requireNotNull(decoded)
        assertThat(encoded).isNotEqualTo(PLAIN_TEXT)
        assertThat(decoded).isEqualTo(PLAIN_TEXT)
    }
}