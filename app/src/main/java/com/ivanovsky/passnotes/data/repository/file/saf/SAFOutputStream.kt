package com.ivanovsky.passnotes.data.repository.file.saf

import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class SAFOutputStream(
    private val destination: OutputStream
) : OutputStream() {

    private val isFlushed = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)
    private val isFailed = AtomicBoolean(false)

    override fun write(b: Int) {
        if (isFailed.get() || isClosed.get()) {
            return
        }

        try {
            destination.write(b)
            isFlushed.set(false)
        } catch (e: IOException) {
            Timber.d(e)
            isFailed.set(true)
            throw IOException(e)
        }
    }

    override fun flush() {
        if (isFailed.get() || isClosed.get()) {
            return
        }

        try {
            destination.flush()
            isFlushed.set(true)
        } catch (e: IOException) {
            Timber.d(e)
            isFailed.set(true)
            throw IOException(e)
        }
    }

    override fun close() {
        if (isFailed.get() || isClosed.get()) {
            return
        }

        if (!isFlushed.get()) {
            flush()
        }

        try {
            destination.close()
            isClosed.set(true)
        } catch (e: IOException) {
            Timber.d(e)
            isFailed.set(true)
            throw IOException(e)
        }
    }
}