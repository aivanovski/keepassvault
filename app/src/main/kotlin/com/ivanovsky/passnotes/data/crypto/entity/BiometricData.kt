package com.ivanovsky.passnotes.data.crypto.entity

class BiometricData(
    val initVector: ByteArray,
    val encryptedData: ByteArray
)