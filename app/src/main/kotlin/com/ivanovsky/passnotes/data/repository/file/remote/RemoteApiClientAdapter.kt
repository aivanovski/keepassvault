package com.ivanovsky.passnotes.data.repository.file.remote

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.Type
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSAuthException
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSFileNotFoundException
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSNetworkException

class RemoteApiClientAdapter(
    val baseClient: RemoteApiClientV2
) : RemoteApiClient {

    override fun listFiles(dir: FileDescriptor): List<FileDescriptor> {
        val files = baseClient.listFiles(dir)
        if (files.isFailed) {
            throwExceptionFromError(files.error)
        }

        return files.obj
    }

    override fun getParent(file: FileDescriptor): FileDescriptor {
        val parent = baseClient.getParent(file)
        if (parent.isFailed) {
            throwExceptionFromError(parent.error)
        }

        return parent.obj
    }

    override fun getRoot(): FileDescriptor {
        val root = baseClient.getRoot()
        if (root.isFailed) {
            throwExceptionFromError(root.error)
        }

        return root.obj
    }

    override fun getFileMetadataOrThrow(file: FileDescriptor): RemoteFileMetadata {
        val metadata = baseClient.getFileMetadata(file)
        if (metadata.isFailed) {
            throwExceptionFromError(metadata.error)
        }

        return metadata.obj
    }

    override fun downloadFileOrThrow(
        remotePath: String,
        destinationPath: String
    ): RemoteFileMetadata {
        val download = baseClient.downloadFile(remotePath, destinationPath)
        if (download.isFailed) {
            throwExceptionFromError(download.error)
        }

        return download.obj
    }

    override fun uploadFileOrThrow(remotePath: String, localPath: String): RemoteFileMetadata {
        val upload = baseClient.uploadFile(remotePath, localPath)
        if (upload.isFailed) {
            throwExceptionFromError(upload.error)
        }

        return upload.obj
    }

    @Throws(RemoteFSException::class)
    private fun throwExceptionFromError(error: OperationError) {
        when (error.type) {
            Type.AUTH_ERROR -> throw RemoteFSAuthException()
            Type.FILE_NOT_FOUND_ERROR -> throw RemoteFSFileNotFoundException(error.message)
            Type.NETWORK_IO_ERROR -> throw RemoteFSNetworkException()
            else -> throw RemoteFSException(error.message)
        }
    }
}