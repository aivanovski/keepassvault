package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import java.security.MessageDigest

object ShaUtils {

    private const val SHA_256 = "SHA-256"

    fun sha256(bytes: ByteArray): Hash {
        val digest = MessageDigest.getInstance(SHA_256)
        val sha = digest.digest(bytes)
        return Hash(sha, HashType.SHA_256)
    }
}