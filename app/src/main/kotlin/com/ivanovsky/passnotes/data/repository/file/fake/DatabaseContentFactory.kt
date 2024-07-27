package com.ivanovsky.passnotes.data.repository.file.fake

fun interface DatabaseContentFactory {
    fun create(): ByteArray
}