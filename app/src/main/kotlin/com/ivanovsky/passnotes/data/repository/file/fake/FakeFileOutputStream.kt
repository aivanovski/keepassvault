package com.ivanovsky.passnotes.data.repository.file.fake

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import timber.log.Timber

class FakeFileOutputStream(
    private val onFinished: (bytes: ByteArray) -> Unit
) : OutputStream() {

    private val out = ByteArrayOutputStream()
    private var isFailed = false
    private var isClosed = false

    override fun write(b: Int) {
        throwIfInvalidState()

        try {
            out.write(b)
        } catch (exception: IOException) {
            Timber.d(exception)
            isFailed = true
            throw IOException(exception)
        }
    }

    override fun flush() {
        throwIfInvalidState()
    }

    override fun close() {
        if (isClosed || isFailed) {
            return
        }

        val bytes = try {
            out.toByteArray()
        } catch (exception: IOException) {
            Timber.d(exception)
            isFailed = true
            throw IOException(exception)
        }

        onFinished.invoke(bytes)
    }

    private fun throwIfInvalidState() {
        when {
            isFailed -> throw IOException("Invalid state: failed")
            isClosed -> throw IOException("Invalid state: closed")
        }
    }
}