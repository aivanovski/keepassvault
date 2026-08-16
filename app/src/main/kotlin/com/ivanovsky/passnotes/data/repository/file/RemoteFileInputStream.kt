package com.ivanovsky.passnotes.data.repository.file

import java.io.IOException
import java.io.InputStream

class RemoteFileInputStream(
    val path: String,
    private val source: InputStream
) : InputStream() {

    override fun read(): Int = source.read()
    override fun skip(n: Long): Long = source.skip(n)
    override fun mark(readlimit: Int) = source.mark(readlimit)
    override fun markSupported(): Boolean = source.markSupported()
    override fun reset() = source.reset()
    override fun available(): Int = source.available()

    @Throws(IOException::class)
    override fun close() = source.close()
}