package com.ivanovsky.passnotes.domain.otp.model

enum class HashAlgorithmType {
    SHA1,
    SHA256,
    SHA512;

    companion object {

        fun fromString(name: String): HashAlgorithmType? {
            val loweredName = name.lowercase()
            return entries.firstOrNull { algorithm -> loweredName == algorithm.name }
        }
    }
}