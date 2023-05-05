package com.ivanovsky.passnotes.domain.interactor.storagelist

import android.net.Uri
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.saf.SAFHelper
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.model.StorageOptionType.EXTERNAL_STORAGE
import com.ivanovsky.passnotes.presentation.storagelist.model.StorageOptionType.GIT
import com.ivanovsky.passnotes.presentation.storagelist.model.StorageOptionType.PRIVATE_STORAGE
import com.ivanovsky.passnotes.presentation.storagelist.model.StorageOptionType.SAF_STORAGE
import com.ivanovsky.passnotes.presentation.storagelist.model.StorageOptionType.WEBDAV
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import kotlinx.coroutines.withContext

class StorageListInteractor(
    private val resourceProvider: ResourceProvider,
    private val fileSystemResolver: FileSystemResolver,
    private val safHelper: SAFHelper,
    private val dispatchers: DispatcherProvider
) {

    fun setupPermissionForSaf(uri: Uri): OperationResult<Unit> {
        return safHelper.setupPermissionIfNeed(uri)
    }

    suspend fun getStorageOptions(action: Action): OperationResult<List<StorageOption>> =
        withContext(dispatchers.IO) {
            val getInternalRootResult = fileSystemResolver
                .resolveProvider(FSAuthority.INTERNAL_FS_AUTHORITY)
                .rootFile
            if (getInternalRootResult.isFailed) {
                return@withContext getInternalRootResult.takeError()
            }

            val getExternalRootResult = fileSystemResolver
                .resolveProvider(FSAuthority.EXTERNAL_FS_AUTHORITY)
                .rootFile

            val internalRoot = getInternalRootResult.obj
            val externalRoot = if (getExternalRootResult.isSucceeded) {
                getExternalRootResult.obj
            } else {
                null
            }

            val options = mutableListOf(
                createPrivateStorageOption(internalRoot),
                createSafStorageOption()
            )

            if (externalRoot != null) {
                options.add(createExternalStorageOption(externalRoot))
            }

            options.add(createWebDavOption())
            options.add(createGitOption())

            OperationResult.success(options)
        }

    private fun createPrivateStorageOption(root: FileDescriptor): StorageOption {
        return StorageOption(
            PRIVATE_STORAGE,
            resourceProvider.getString(R.string.private_app_storage),
            root
        )
    }

    private fun createExternalStorageOption(root: FileDescriptor): StorageOption {
        return StorageOption(
            EXTERNAL_STORAGE,
            resourceProvider.getString(R.string.external_storage_app_picker),
            root
        )
    }

    private fun createSafStorageOption(): StorageOption {
        return StorageOption(
            SAF_STORAGE,
            resourceProvider.getString(R.string.external_storage_system_picker),
            createSafStorageDir()
        )
    }

    private fun createWebDavOption(): StorageOption {
        return StorageOption(
            WEBDAV,
            resourceProvider.getString(R.string.webdav),
            createWebdavStorageDir()
        )
    }

    private fun createGitOption(): StorageOption {
        return StorageOption(
            GIT,
            resourceProvider.getString(R.string.git),
            createGitStorageDir()
        )
    }

    private fun createSafStorageDir(): FileDescriptor {
        return FileDescriptor(
            fsAuthority = FSAuthority.SAF_FS_AUTHORITY,
            path = ROOT_PATH,
            uid = ROOT_PATH,
            name = ROOT_PATH,
            isDirectory = true,
            isRoot = true
        )
    }

    private fun createWebdavStorageDir(): FileDescriptor =
        FileDescriptor(
            fsAuthority = FSAuthority(
                credentials = null,
                type = FSType.WEBDAV
            ),
            path = ROOT_PATH,
            uid = ROOT_PATH,
            name = ROOT_PATH,
            isDirectory = true,
            isRoot = true
        )

    private fun createGitStorageDir(): FileDescriptor =
        FileDescriptor(
            fsAuthority = FSAuthority(
                credentials = null,
                type = FSType.GIT
            ),
            path = ROOT_PATH,
            uid = ROOT_PATH,
            name = ROOT_PATH,
            isDirectory = true,
            isRoot = true
        )

    suspend fun getFileSystemRoot(
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(fsAuthority)
                .rootFile
        }

    suspend fun getFileByPath(
        path: String,
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(fsAuthority)
                .getFile(path, FSOptions.DEFAULT)
        }
}