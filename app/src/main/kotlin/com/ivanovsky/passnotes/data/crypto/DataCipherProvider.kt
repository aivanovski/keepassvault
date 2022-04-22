package com.ivanovsky.passnotes.data.crypto

interface DataCipherProvider {
    fun getCipher(): DataCipher
}