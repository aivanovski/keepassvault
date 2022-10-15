package com.ivanovsky.passnotes.data.crypto.entity

data class Base64SecretData(
    val initVector: String,
    val encryptedText: String
) {

    override fun toString(): String {
        return "$initVector$INIT_VECTOR_SEPARATOR$encryptedText"
    }

    companion object {

        private const val INIT_VECTOR_SEPARATOR = "|"

        fun parse(data: String): Base64SecretData? {
            var result: Base64SecretData? = null

            if (data.isNotEmpty()) {
                val separatorIdx = data.indexOf(INIT_VECTOR_SEPARATOR)
                if (separatorIdx > 0 && separatorIdx + 1 <= data.length) {
                    val initVector = data.substring(0, separatorIdx)
                    val cipherText = data.substring(separatorIdx + 1)
                    result = Base64SecretData(initVector, cipherText)
                }
            }

            return result
        }
    }
}