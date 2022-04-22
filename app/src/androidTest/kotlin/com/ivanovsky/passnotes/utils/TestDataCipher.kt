package com.ivanovsky.passnotes.utils

import com.ivanovsky.passnotes.data.crypto.DataCipher

class TestDataCipher : DataCipher {

    override fun encode(data: String): String? {
        val chars = data.map {
            if (it == Char.MAX_VALUE) {
                0.toChar()
            } else {
                it + 1
            }
        }

        return String(chars.toCharArray())
    }

    override fun decode(data: String): String? {
        val chars = data.map {
            if (it == 0.toChar()) {
                Char.MAX_VALUE
            } else {
                it - 1
            }
        }
        return String(chars.toCharArray())
    }
}