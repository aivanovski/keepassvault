package com.ivanovsky.passnotes.domain.otp.model

enum class HashAlgorithmType(val rfcName: String) {
    SHA1(rfcName = "SHA1"),
    SHA256(rfcName = "SHA256"),
    SHA512(rfcName = "SHA512");

    companion object {

        fun fromString(name: String): HashAlgorithmType? {
            return entries.firstOrNull { algorithm ->
                name.equals(
                    algorithm.name,
                    ignoreCase = true
                )
            }
        }
    }
}