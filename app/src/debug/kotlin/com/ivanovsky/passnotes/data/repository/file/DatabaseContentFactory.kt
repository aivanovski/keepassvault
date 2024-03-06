package com.ivanovsky.passnotes.data.repository.file

fun interface DatabaseContentFactory {
    fun create(): ByteArray
}