package com.ivanovsky.passnotes.data.repository.file

import java.io.IOException
import java.io.InputStream

class RemoteFileInputStream(
    val path: String,
    private val source: InputStream
) : InputStream() {

    override fun read(): Int {
        return source.read()
    }

    override fun skip(n: Long): Long {
        return source.skip(n)
    }

    override fun mark(readlimit: Int) {
        source.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return source.markSupported()
    }

    override fun reset() {
        source.reset()
    }

    override fun available(): Int {
        return source.available()
    }

    @Throws(IOException::class)
    override fun close() {
        source.close()
    }
}