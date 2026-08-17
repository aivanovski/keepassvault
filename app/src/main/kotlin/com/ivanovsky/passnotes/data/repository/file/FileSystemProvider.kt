package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import java.io.InputStream
import java.io.OutputStream

interface FileSystemProvider {

    val authenticator: FileSystemAuthenticator
    val syncProcessor: FileSystemSyncProcessor

    fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>>
    fun getParent(file: FileDescriptor): OperationResult<FileDescriptor>
    fun getRootFile(): OperationResult<FileDescriptor>
    fun exists(file: FileDescriptor): OperationResult<Boolean>
    fun getFile(path: String, options: FSOptions): OperationResult<FileDescriptor>

    fun openFileForRead(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<InputStream>

    fun openFileForWrite(
        file: FileDescriptor,
        onConflictStrategy: OnConflictStrategy,
        options: FSOptions
    ): OperationResult<OutputStream>
}