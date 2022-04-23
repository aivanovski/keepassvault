package com.ivanovsky.passnotes.data.crypto

interface DataCipher {
    fun encode(data: String): String?
    fun decode(data: String): String?
}