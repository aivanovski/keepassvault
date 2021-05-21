package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata

interface RemoteApiClientV2 {
    fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>>
    fun getParent(file: FileDescriptor): OperationResult<FileDescriptor>
    fun getRoot(): OperationResult<FileDescriptor>
    fun getFileMetadata(file: FileDescriptor): OperationResult<RemoteFileMetadata>
    fun downloadFile(remotePath: String, destinationPath: String): OperationResult<RemoteFileMetadata>
    fun uploadFile(remotePath: String, localPath: String): OperationResult<RemoteFileMetadata>
}