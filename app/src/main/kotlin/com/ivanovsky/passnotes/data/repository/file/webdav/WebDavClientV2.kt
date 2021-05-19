package com.ivanovsky.passnotes.data.repository.file.webdav

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_PARENT_PATH
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_IS_NOT_A_DIRECTORY
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_IO_ERROR
import com.ivanovsky.passnotes.data.entity.OperationError.newAuthError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileNotFoundError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClientV2
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.Logger
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.thegrizzlylabs.sardineandroid.DavResource
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient

class WebDavClientV2(
    private val authenticator: WebdavAuthenticator,
    httpClient: OkHttpClient
) : RemoteApiClientV2 {

    private val webDavClient = WebDavNetworkLayer(httpClient).apply {
        val creds = authenticator.getFsAuthority().credentials
        if (creds != null) {
            setCredentials(creds)
        }
    }
    private var fsAuthority = authenticator.getFsAuthority()

    override fun listFiles(dir: FileDescriptor): OperationResult<List<FileDescriptor>> {
        Logger.d(TAG, "listFiles: dir=$dir")

        if (!dir.isDirectory) {
            return OperationResult.error(newFileAccessError(MESSAGE_FILE_IS_NOT_A_DIRECTORY))
        }

        val files = fetchFileList(dir.path)
        if (files.isFailed) {
            return files.takeError()
        }

        return OperationResult.success(files.obj.excludeByPath(dir.path))
    }

    override fun getParent(file: FileDescriptor): OperationResult<FileDescriptor> {
        Logger.d(TAG, "getParent: file=$file")

        val checkAuthority = checkFsAuthority(file)
        if (checkAuthority.isFailed) {
            return checkAuthority.takeError()
        }

        val parentPath = FileUtils.getParentPath(file.path)
            ?: return OperationResult.error(newFileAccessError(MESSAGE_FAILED_TO_GET_PARENT_PATH))

        val files = fetchFileList(parentPath)
        if (files.isFailed) {
            return files.takeError()
        }

        val parentFile = files.obj.firstOrNull { it.path == parentPath }
            ?: return OperationResult.error(newFileNotFoundError())

        return OperationResult.success(parentFile)
    }

    override fun getRoot(): OperationResult<FileDescriptor> {
        Logger.d(TAG, "getRoot:")
        val files = fetchFileList(EMPTY)
        if (files.isFailed) {
            return files.takeError()
        }

        val root = files.obj.firstOrNull { it.isRoot }
            ?: return OperationResult.error(newFileNotFoundError())

        return OperationResult.success(root)
    }

    override fun getFileMetadata(file: FileDescriptor): OperationResult<RemoteFileMetadata> {
        val checkAuthority = checkFsAuthority(file)
        if (checkAuthority.isFailed) {
            return checkAuthority.takeError()
        }

        return getFileMetadata(file.path)
    }

    private fun getFileMetadata(path: String): OperationResult<RemoteFileMetadata> {
        Logger.d(TAG, "getFileMetadata: path=%s", path)
        val metadata = fetchDavResource(path)
        if (metadata.isFailed) {
            return metadata.takeError()
        }

        return OperationResult.success(metadata.obj.toRemoteFileMetadata())
    }

    override fun downloadFile(
        remotePath: String,
        destinationPath: String
    ): OperationResult<RemoteFileMetadata> {
        val input = webDavClient.execute { client ->
            client.get(formatUrl(remotePath))
        }
        if (input.isFailed) {
            return input.takeError()
        }

        val cancellation = AtomicBoolean(false)
        try {
            val out = FileOutputStream(File(destinationPath))
            InputOutputUtils.copy(input.obj, out, true, cancellation)
        } catch (e: IOException) {
            Logger.printStackTrace(e)
            cancellation.set(true)
            return OperationResult.error(newGenericError(MESSAGE_IO_ERROR))
        }

        return getFileMetadata(remotePath)
    }

    override fun uploadFile(
        remotePath: String,
        localPath: String
    ): OperationResult<RemoteFileMetadata> {
        val put = webDavClient.execute { client ->
            client.put(formatUrl(remotePath), File(localPath), CONTENT_TYPE)
        }
        if (put.isFailed) {
            return put.takeError()
        }

        return getFileMetadata(remotePath)
    }

    private fun checkCredentials(): OperationResult<Unit> {
        val authenticatorCreds = authenticator.getFsAuthority().credentials
        val currentCreds = fsAuthority.credentials

        if (currentCreds != authenticatorCreds && authenticatorCreds != null) {
            fsAuthority = authenticator.getFsAuthority()
            webDavClient.setCredentials(authenticatorCreds)
        }

        if (fsAuthority.credentials == null) {
            return OperationResult.error(newAuthError())
        }

        return OperationResult.success(Unit)
    }

    private fun checkFsAuthority(file: FileDescriptor): OperationResult<Unit> {
        if (file.fsAuthority != fsAuthority) {
            return OperationResult.error(newAuthError(MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS))
        }

        return OperationResult.success(Unit)
    }

    private fun fetchFileList(path: String): OperationResult<List<FileDescriptor>> {
        val checkCreds = checkCredentials()
        if (checkCreds.isFailed) {
            return checkCreds.takeError()
        }

        val url = formatUrl(path)
        Logger.d(TAG, "fetchFileList: url=%s, cred=%s", url, fsAuthority.credentials)
        val files = webDavClient.execute { client ->
            client
                .list(url)
                .toFileDescriptors()
        }
        if (files.isFailed) {
            return files.takeError()
        }

        return files
    }

    private fun fetchDavResource(path: String): OperationResult<DavResource> {
        val checkCreds = checkCredentials()
        if (checkCreds.isFailed) {
            return checkCreds.takeError()
        }

        Logger.d(TAG, "fetchDavResource: path=%s", path)
        val data = webDavClient.execute { client ->
            client
                .list(formatUrl(path))
                .firstOrNull()
        }

        if (data.isFailed) {
            return data.takeError()
        }

        if (data.obj == null) {
            return OperationResult.error(newFileNotFoundError())
        }

        return OperationResult.success(data.obj)
    }

    private fun formatUrl(path: String): String {
        return getServerUrl() + path
    }

    private fun getServerUrl(): String {
        return fsAuthority.credentials?.serverUrl ?: throw IllegalStateException()
    }

    private fun List<DavResource>.toFileDescriptors(): List<FileDescriptor> {
        return this.map { it.toFileDescriptor() }
    }

    private fun List<FileDescriptor>.excludeByPath(excludePath: String): List<FileDescriptor> {
        return this.filter { it.path != excludePath }
    }

    private fun DavResource.toFileDescriptor(): FileDescriptor {
        val path = FileUtils.removeSeparatorIfNeed(href.toString())
        return FileDescriptor(
            fsAuthority = fsAuthority,
            path = path,
            uid = path,
            isDirectory = isDirectory,
            isRoot = (path == ROOT_PATH),
            modified = modified.time
        )
    }

    private fun DavResource.toRemoteFileMetadata(): RemoteFileMetadata {
        val path = FileUtils.removeSeparatorIfNeed(href.toString())
        return RemoteFileMetadata(
            uid = path,
            path = path,
            serverModified = this.modified,
            clientModified = this.modified,
            revision = this.etag
        )
    }

    companion object {
        private const val CONTENT_TYPE = "application/octet-stream"
        private val TAG = WebDavClientV2::class.simpleName
    }
}