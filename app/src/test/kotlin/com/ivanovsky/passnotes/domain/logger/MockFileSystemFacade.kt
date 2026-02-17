package com.ivanovsky.passnotes.domain.logger

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import okio.IOException

class MockFileSystemFacade(
    rootDir: File,
    private val writers: Map<File, Writer> = emptyMap()
) : FileSystemFacade {

    var lastWriter: Writer? = null

    private val underlyingFacade = FileSystemFacadeImpl(rootDir)

    override fun getRootDir(): File =
        underlyingFacade.getRootDir()

    override fun resolve(path: String): File =
        underlyingFacade.resolve(path)

    override fun ensureDirectory(dir: File) =
        underlyingFacade.ensureDirectory(dir)

    override fun delete(file: File) =
        underlyingFacade.delete(file)

    override fun listFiles(dir: File): List<File> =
        underlyingFacade.listFiles(dir)

    override fun fileSize(file: File): Long =
        underlyingFacade.fileSize(file)

    override fun openWriter(file: File): Writer {
        val writer = writers[file] ?: MockWriter(file)
        lastWriter = writer
        return writer
    }

    class MockWriter(
        private val file: File
    ) : Writer() {

        var isFlushed = true
        var isClosed = false
        private val out = BufferedWriter(FileWriter(file, true))

        override fun write(cbuf: CharArray?, off: Int, len: Int) {
            out.write(cbuf, off, len)
            isFlushed = false
        }

        override fun flush() {
            out.flush()
            isFlushed = true
        }

        override fun close() {
            out.close()
            isClosed = true
        }
    }

    class ThrowOnWriteWriter(
        private val file: File,
        private val failedWrites: Int = Int.MAX_VALUE
    ) : Writer() {

        private var writes = 0
        private var out: BufferedWriter? = null

        override fun close() {
            out?.close()
            out = null
        }

        override fun flush() {
            out?.flush()
        }

        override fun write(cbuf: CharArray?, off: Int, len: Int) {
            writes++
            if (writes <= failedWrites) {
                throw IOException()
            } else {
                ensureOutputOpen()
                out?.write(cbuf, off, len)
            }
        }

        private fun ensureOutputOpen() {
            if (out == null) {
                out = BufferedWriter(FileWriter(file, true))
            }
        }
    }
}