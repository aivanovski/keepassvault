package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.data.repository.file.BaseRemoteFileOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import timber.log.Timber

class OfflineFileOutputStream(
    private val provider: RemoteFileSystemProvider,
    private val file: RemoteFile,
    private val processingUnitUid: UUID
) : BaseRemoteFileOutputStream() {

    override val outputFile = File(file.localPath)

    @Volatile
    private var failed = false

    @Volatile
    private var flushed = false

    @Volatile
    private var closed = false

    private val out: OutputStream = BufferedOutputStream(FileOutputStream(outputFile))

    @Throws(IOException::class)
    override fun write(b: Int) {
        try {
            out.write(b)
            flushed = false
        } catch (exception: IOException) {
            Timber.d(exception)
            failed = true
            provider.onOfflineWriteFailed(file, processingUnitUid)
            throw IOException(exception)
        }
    }

    @Throws(IOException::class)
    override fun flush() {
        try {
            out.flush()
            flushed = true
        } catch (exception: IOException) {
            Timber.d(exception)
            failed = true
            provider.onOfflineWriteFailed(file, processingUnitUid)
            throw IOException(exception)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        if (failed || closed) return

        if (!flushed) {
            flush()
        }

        closed = true
        provider.onOfflineWriteFinished(file, processingUnitUid)
    }
}