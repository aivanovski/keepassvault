package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException

@Deprecated("RemoteApiClientV2 should be used instead")
interface RemoteApiClient {

    @Deprecated("RemoteApiClientV2 should be used instead")
    @Throws(RemoteFSException::class)
    fun listFiles(dir: FileDescriptor): List<FileDescriptor>

    @Deprecated("RemoteApiClientV2 should be used instead")
    @Throws(RemoteFSException::class)
    fun getParent(file: FileDescriptor): FileDescriptor

    @Deprecated("RemoteApiClientV2 should be used instead")
    @Throws(RemoteFSException::class)
    fun getRoot(): FileDescriptor

    @Deprecated("RemoteApiClientV2 should be used instead")
    @Throws(RemoteFSException::class)
    fun getFileMetadataOrThrow(file: FileDescriptor): RemoteFileMetadata

    @Deprecated("RemoteApiClientV2 should be used instead")
    @Throws(RemoteFSException::class)
    fun downloadFileOrThrow(remotePath: String, destinationPath: String): RemoteFileMetadata

    @Deprecated("RemoteApiClientV2 should be used instead")
    @Throws(RemoteFSException::class)
    fun uploadFileOrThrow(remotePath: String, localPath: String): RemoteFileMetadata
}