package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.RemoteFile
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.file.BaseRemoteFileOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import timber.log.Timber

class RemoteFileOutputStream(
    private val provider: RemoteFileSystemProvider,
    private val client: RemoteApiClient,
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

    // Lazily initialized because FileOutputStream clears the file content.
    @Volatile
    private var out: OutputStream? = null

    @Throws(IOException::class)
    override fun write(b: Int) {
        if (failed) return

        if (out == null) {
            out = BufferedOutputStream(FileOutputStream(outputFile))
        }

        try {
            out?.write(b)
            flushed = false
        } catch (e: IOException) {
            Timber.d(e)
            failed = true
            provider.onFileUploadFailed(file, processingUnitUid)
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun flush() {
        val out = out ?: return

        if (failed) return

        try {
            out.flush()
            flushed = true
        } catch (e: IOException) {
            Timber.d(e)
            failed = true
            provider.onFileUploadFailed(file, processingUnitUid)
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        val out = out ?: return

        if (failed || closed) return

        if (!flushed) {
            flush()
        }

        try {
            out.close()
        } catch (e: IOException) {
            failed = true
            provider.onFileUploadFailed(file, processingUnitUid)
            throw IOException(e)
        }

        val uploadResult = client.uploadFile(file.remotePath, file.localPath)
        if (uploadResult.isFailed) {
            val error = uploadResult.error
            Timber.d("Failed to upload file: %s", error)
            if (error.type == OperationError.Type.NETWORK_IO_ERROR) {
                provider.onOfflineWriteFinished(file, processingUnitUid)
            } else {
                provider.onFileUploadFailed(file, processingUnitUid)
            }
            throw IOException(error.message)
        }

        val metadata: RemoteFileMetadata = uploadResult.obj
        closed = true

        provider.onFileUploadFinished(file, metadata, processingUnitUid)
    }
}