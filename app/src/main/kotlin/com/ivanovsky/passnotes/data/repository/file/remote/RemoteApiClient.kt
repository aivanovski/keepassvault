package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.entity.RemoteFolderMetadata
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxException

internal interface RemoteApiClient {

    @Throws(DropboxException::class)
    fun listFiles(dir: FileDescriptor): List<FileDescriptor>

    @Throws(DropboxException::class)
    fun getParent(file: FileDescriptor): FileDescriptor

    @Throws(DropboxException::class)
    fun getRoot(): FileDescriptor

    @Throws(DropboxException::class)
    fun getFileMetadataOrThrow(file: FileDescriptor): RemoteFileMetadata

    @Throws(DropboxException::class)
    fun downloadFileOrThrow(remotePath: String, destinationPath: String): RemoteFileMetadata

    @Throws(DropboxException::class)
    fun getFolderMetadataOrThrow(path: String): RemoteFolderMetadata

    @Throws(DropboxException::class)
    fun uploadFileOrThrow(remotePath: String, destinationPath: String): RemoteFileMetadata
}