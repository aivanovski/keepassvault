package com.ivanovsky.passnotes.domain.interactor.storagelist

import android.net.Uri
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.saf.SAFHelper
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import kotlinx.coroutines.withContext

class StorageListInteractor(
    private val fileSystemResolver: FileSystemResolver,
    private val safHelper: SAFHelper,
    private val dispatchers: DispatcherProvider
) {

    private val optionFactories = mapOf(
        FSType.INTERNAL_STORAGE to { createPrivateStorageOption() },
        FSType.EXTERNAL_STORAGE to { createExternalStorageOption() },
        FSType.SAF to { createSafStorageOption() },
        FSType.WEBDAV to { createWebDavOption() },
        FSType.GIT to { createGitOption() },
        FSType.FAKE to { createFakeFsOption() }
    )

    fun setupPermissionForSaf(uri: Uri): OperationResult<Unit> {
        return safHelper.setupPermissionIfNeed(uri)
    }

    suspend fun getStorageOptions(action: Action): OperationResult<List<StorageOption>> =
        withContext(dispatchers.IO) {
            val availableFsTypes = fileSystemResolver.getAvailableFsTypes()

            val options = mutableListOf<StorageOption>()

            for (fsType in SORTED_FS_TYPES) {
                if (fsType !in availableFsTypes) {
                    continue
                }

                val optionFactory = optionFactories[fsType] ?: continue

                val option = withContext(dispatchers.IO) {
                    optionFactory.invoke()
                } ?: continue

                options.add(option)
            }

            OperationResult.success(options)
        }

    private fun createPrivateStorageOption(): StorageOption? {
        val getRootResult = fileSystemResolver
            .resolveProvider(FSAuthority.INTERNAL_FS_AUTHORITY)
            .rootFile

        if (getRootResult.isFailed) {
            return null
        }

        val root = getRootResult.getOrThrow()
        return StorageOption(
            root
        )
    }

    private fun createExternalStorageOption(): StorageOption? {
        val getRootResult = fileSystemResolver
            .resolveProvider(FSAuthority.EXTERNAL_FS_AUTHORITY)
            .rootFile

        if (getRootResult.isFailed) {
            return null
        }

        val root = getRootResult.getOrThrow()
        return StorageOption(
            root
        )
    }

    private fun createSafStorageOption(): StorageOption {
        return StorageOption(
            root = FileDescriptor(
                fsAuthority = FSAuthority.SAF_FS_AUTHORITY,
                path = ROOT_PATH,
                uid = ROOT_PATH,
                name = ROOT_PATH,
                isDirectory = true,
                isRoot = true
            )
        )
    }

    private fun createWebDavOption(): StorageOption {
        return StorageOption(
            root = FileDescriptor(
                fsAuthority = FSAuthority(
                    credentials = null,
                    type = FSType.WEBDAV,
                    isBrowsable = true
                ),
                path = ROOT_PATH,
                uid = ROOT_PATH,
                name = ROOT_PATH,
                isDirectory = true,
                isRoot = true
            )
        )
    }

    private fun createGitOption(): StorageOption {
        return StorageOption(
            root = FileDescriptor(
                fsAuthority = FSAuthority(
                    credentials = null,
                    type = FSType.GIT,
                    isBrowsable = true
                ),
                path = ROOT_PATH,
                uid = ROOT_PATH,
                name = ROOT_PATH,
                isDirectory = true,
                isRoot = true
            )
        )
    }

    private fun createFakeFsOption(): StorageOption {
        return StorageOption(
            FileDescriptor(
                fsAuthority = FSAuthority(
                    credentials = null,
                    type = FSType.FAKE,
                    isBrowsable = true
                ),
                path = ROOT_PATH,
                uid = ROOT_PATH,
                name = ROOT_PATH,
                isDirectory = true,
                isRoot = true
            )
        )
    }

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

    companion object {
        private val SORTED_FS_TYPES = listOf(
            FSType.INTERNAL_STORAGE,
            FSType.SAF,
            FSType.EXTERNAL_STORAGE,
            FSType.WEBDAV,
            FSType.GIT,
            FSType.FAKE
        )
    }
}