package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.util.StringUtils.splitIntoWords

object OtpCodeFormatter {

    fun format(code: String): String {
        val length = code.length

        return when {
            length < 6 -> code
            length % 3 == 0 -> splitIntoWords(code, wordLength = 3)
            length % 4 == 0 -> splitIntoWords(code, wordLength = 4)
            length == 7 -> splitIntoWords(code, wordLength = 4)
            else -> splitIntoWords(code, wordLength = 4)
        }
    }
}