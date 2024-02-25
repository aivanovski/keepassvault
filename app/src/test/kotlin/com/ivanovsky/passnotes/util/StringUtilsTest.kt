package com.ivanovsky.passnotes.util

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.util.StringUtils.splitIntoWords
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `splitIntoWords should work`() {
        // normal cases
        assertThat(splitIntoWords("123456", 3)).isEqualTo("123 456")
        assertThat(splitIntoWords("1234", 4)).isEqualTo("1234")
        assertThat(splitIntoWords("12345678", 4)).isEqualTo("1234 5678")

        // corner cases
        assertThat(splitIntoWords("", 3)).isEqualTo("")
        assertThat(splitIntoWords("12345", 3)).isEqualTo("123 45")
    }
}