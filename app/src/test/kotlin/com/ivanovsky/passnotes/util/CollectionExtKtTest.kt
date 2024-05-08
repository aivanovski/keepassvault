package com.ivanovsky.passnotes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CollectionExtKtTest {

    @Test
    fun `splitAt should split collection at specified index`() {
        listOf(
            DATA.splitAt(0) to Pair(emptyList(), listOf(1, 2, 3)),
            DATA.splitAt(1) to Pair(listOf(1), listOf(2, 3)),
            DATA.splitAt(2) to Pair(listOf(1, 2), listOf(3)),
            DATA.splitAt(3) to Pair(listOf(1, 2, 3), emptyList()),
            emptyList<Int>().splitAt(0) to Pair(emptyList(), emptyList())
        )
            .forEach { (result, expected) ->
                assertThat(result).isEqualTo(expected)
            }
    }

    @Test(expected = ArrayIndexOutOfBoundsException::class)
    fun `splitAt should throw exception if index is negative`() {
        DATA.splitAt(-1)
    }

    @Test(expected = ArrayIndexOutOfBoundsException::class)
    fun `splitAt should throw exception if index too big`() {
        DATA.splitAt(DATA.size + 1)
    }

    companion object {
        private val DATA = listOf(1, 2, 3)
    }
}