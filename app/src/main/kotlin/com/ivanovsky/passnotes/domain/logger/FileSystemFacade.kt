package com.ivanovsky.passnotes.domain.logger

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

interface FileSystemFacade {
    fun getRootDir(): File
    fun resolve(path: String): File
    fun ensureDirectory(dir: File)
    fun delete(file: File)
    fun listFiles(dir: File): List<File>
    fun fileSize(file: File): Long
    fun openWriter(file: File): Writer
}

class FileSystemFacadeImpl(
    private val rootDir: File
) : FileSystemFacade {

    override fun getRootDir(): File = rootDir

    override fun resolve(path: String): File =
        File(rootDir, path)

    override fun ensureDirectory(dir: File) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    override fun delete(file: File) {
        if (file.isFile && file.exists()) {
            file.delete()
        }
    }

    override fun listFiles(dir: File): List<File> =
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }

    override fun fileSize(file: File): Long =
        if (file.exists()) file.length() else 0

    override fun openWriter(file: File): Writer =
        WatchingWriter(file)

    class WatchingWriter(
        private val file: File
    ) : Writer() {

        private var isExistBefore = AtomicBoolean(file.exists())
        private var out = AtomicReference(BufferedWriter(FileWriter(file, true), 16 * 1024))

        override fun close() {
            out.get().close()
        }

        override fun flush() {
            out.get().flush()
            isExistBefore.set(file.exists())
        }

        override fun write(cbuf: CharArray?, off: Int, len: Int) {
            // handle scenario if file was deleted between writes
            if (isExistBefore.get() && !file.exists()) {
                runCatching { out.get().close() }
                out.set(BufferedWriter(FileWriter(file, true), 16 * 1024))
            }

            out.get().write(cbuf, off, len)
        }
    }
}