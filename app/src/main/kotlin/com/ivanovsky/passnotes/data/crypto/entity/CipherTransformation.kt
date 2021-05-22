package com.ivanovsky.passnotes.data.crypto.entity

enum class CipherTransformation(val value: String) {
    AES_CBC_PKCS7("AES/CBC/PKCS7Padding"),
    AES_CBC_PKCS5("AES/CBC/PKCS5Padding")
}