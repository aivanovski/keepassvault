package com.ivanovsky.passnotes.data.crypto.entity

import com.ivanovsky.passnotes.util.Base64Utils.fromBase64String
import com.ivanovsky.passnotes.util.Base64Utils.toBase64String

fun Base64SecretData.toSecretData(): SecretData {
    return SecretData(
        initVector = fromBase64String(initVector),
        encryptedData = fromBase64String(encryptedText)
    )
}

fun SecretData.toBase64SecretData(): Base64SecretData {
    return Base64SecretData(
        initVector = toBase64String(initVector),
        encryptedText = toBase64String(encryptedData)
    )
}

fun SecretData.toBiometricData(): BiometricData =
    BiometricData(
        initVector = initVector,
        encryptedData = encryptedData
    )

fun BiometricData.toSecretData(): SecretData =
    SecretData(
        initVector = initVector,
        encryptedData = encryptedData
    )