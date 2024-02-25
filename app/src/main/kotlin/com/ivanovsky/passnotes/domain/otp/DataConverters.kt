package com.ivanovsky.passnotes.domain.otp

import com.ivanovsky.passnotes.domain.otp.model.HashAlgorithmType
import dev.robinohs.totpkt.otp.HashAlgorithm

fun HashAlgorithmType.toExternalAlgorithm(): HashAlgorithm {
    return when (this) {
        HashAlgorithmType.SHA1 -> HashAlgorithm.SHA1
        HashAlgorithmType.SHA256 -> HashAlgorithm.SHA256
        HashAlgorithmType.SHA512 -> HashAlgorithm.SHA512
    }
}