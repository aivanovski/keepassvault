package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import timber.log.Timber
import kotlin.Throws
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

object InputOutputUtils {

    private const val BUFFER_SIZE = 1024 * 8
    private val UNCANCELABLE = AtomicBoolean(false)

    @JvmStatic
    fun newFileInputStreamOrNull(file: File): FileInputStream? {
        return try {
            FileInputStream(file)
        } catch (e: FileNotFoundException) {
            Timber.d(e)
            null
        }
    }

    @JvmStatic
    fun newFileOutputStreamOrNull(file: File): FileOutputStream? {
        return try {
            FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            Timber.d(e)
            null
        }
    }

    @JvmStatic
    fun copy(
        sourceFile: File,
        destinationFile: File
    ): OperationResult<Unit> {
        val source = try {
            FileInputStream(sourceFile)
        } catch (exception: FileNotFoundException) {
            Timber.d(exception)
            return OperationResult.error(newGenericIOError(exception))
        }

        val destination = try {
            FileOutputStream(destinationFile)
        } catch (exception: FileNotFoundException) {
            Timber.d(exception)
            return OperationResult.error(newGenericIOError(exception))
        }


        return try {
            copy(
                source,
                destination,
                isCloneOnFinish = true,
                cancellation = UNCANCELABLE
            )
            OperationResult.success(Unit)
        } catch (exception: IOException) {
            Timber.d(exception)
            OperationResult.error(newGenericIOError(exception))
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copy(
        source: InputStream,
        destination: OutputStream,
        isCloneOnFinish: Boolean
    ) {
        copy(source, destination, isCloneOnFinish, cancellation = UNCANCELABLE)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copy(
        source: InputStream,
        destination: OutputStream,
        isCloneOnFinish: Boolean,
        cancellation: AtomicBoolean
    ) {
        try {
            val buf = ByteArray(BUFFER_SIZE)
            var len: Int
            while (source.read(buf).also { len = it } > 0 && !cancellation.get()) {
                destination.write(buf, 0, len)
            }
            destination.flush()
        } finally {
            if (isCloneOnFinish) {
                close(source)
                close(destination)
            }
        }
    }

    @JvmStatic
    fun close(stream: OutputStream?) {
        if (stream == null) {
            return
        }

        try {
            stream.close()
        } catch (e: IOException) {
            Timber.d(e)
        }
    }

    @JvmStatic
    fun close(stream: InputStream?) {
        if (stream == null) {
            return
        }

        try {
            stream.close()
        } catch (e: IOException) {
            Timber.d(e)
        }
    }
}