package com.ivanovsky.passnotes.domain.interactor.storagelist

import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.DROPBOX
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.EXTERNAL_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.PRIVATE_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.WEBDAV
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import kotlinx.coroutines.withContext

class StorageListInteractor(
    private val context: Context,
    private val fileSystemResolver: FileSystemResolver,
    private val dispatchers: DispatcherProvider
) {

    fun getStorageOptions(action: Action): List<StorageOption> {
        return when (action) {
            Action.PICK_FILE -> {
                listOf(
                    createPrivateStorageOption(),
                    createExternalStorageOption(),
                    createDropboxOption(),
                    createWebDavOption()
                )
            }
            Action.PICK_STORAGE -> {
                listOf(
                    createPrivateStorageOption(),
                    createDropboxOption(),
                    createWebDavOption()
                )
            }
        }
    }

    private fun createPrivateStorageOption(): StorageOption {
        return StorageOption(
            PRIVATE_STORAGE,
            context.getString(R.string.private_app_storage),
            createPrivateStorageDir()
        )
    }

    private fun createExternalStorageOption(): StorageOption {
        return StorageOption(
            EXTERNAL_STORAGE,
            context.getString(R.string.external_storage_system_picker),
            createExternalStorageDir()
        )
    }

    private fun createDropboxOption(): StorageOption {
        return StorageOption(
            DROPBOX,
            context.getString(R.string.dropbox),
            createDropboxStorageDir()
        )
    }

    private fun createWebDavOption(): StorageOption {
        return StorageOption(
            WEBDAV,
            context.getString(R.string.webdav),
            createWebdavStorageDir()
        )
    }

    private fun createPrivateStorageDir(): FileDescriptor {
        return FileDescriptor.fromRegularFile(context.filesDir)
    }

    private fun createExternalStorageDir(): FileDescriptor {
        return FileDescriptor(
            fsAuthority = FSAuthority.SAF_FS_AUTHORITY,
            path = ROOT_PATH,
            uid = ROOT_PATH,
            name = ROOT_PATH,
            isDirectory = true,
            isRoot = true
        )
    }

    private fun createDropboxStorageDir(): FileDescriptor =
        FileDescriptor(
            fsAuthority = FSAuthority.DROPBOX_FS_AUTHORITY,
            path = ROOT_PATH,
            uid = ROOT_PATH,
            name = ROOT_PATH,
            isDirectory = true,
            isRoot = true
        )

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

    suspend fun getRemoteFileSystemRoot(fsAuthority: FSAuthority): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            val provider = fileSystemResolver.resolveProvider(fsAuthority)
            provider.rootFile
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