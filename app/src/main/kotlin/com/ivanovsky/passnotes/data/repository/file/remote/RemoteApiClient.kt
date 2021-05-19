package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException

interface RemoteApiClient {

    @Throws(RemoteFSException::class)
    fun listFiles(dir: FileDescriptor): List<FileDescriptor>

    @Throws(RemoteFSException::class)
    fun getParent(file: FileDescriptor): FileDescriptor

    @Throws(RemoteFSException::class)
    fun getRoot(): FileDescriptor

    @Throws(RemoteFSException::class)
    fun getFileMetadataOrThrow(file: FileDescriptor): RemoteFileMetadata

    @Throws(RemoteFSException::class)
    fun downloadFileOrThrow(remotePath: String, destinationPath: String): RemoteFileMetadata

    @Throws(RemoteFSException::class)
    fun uploadFileOrThrow(remotePath: String, localPath: String): RemoteFileMetadata
}