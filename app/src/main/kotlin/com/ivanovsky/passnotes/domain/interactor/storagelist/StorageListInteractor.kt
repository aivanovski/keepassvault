package com.ivanovsky.passnotes.domain.interactor.storagelist

import android.content.Context
import android.os.Environment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.DROPBOX
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.EXTERNAL_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.PRIVATE_STORAGE
import com.ivanovsky.passnotes.domain.entity.StorageOptionType.WEBDAV
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageListInteractor(
    private val context: Context,
    private val fileSystemResolver: FileSystemResolver
) {

    fun getAvailableStorageOptions(): List<StorageOption> {
        return listOf(
            createPrivateStorageOption(),
            createExternalStorageOption(),
            createDropboxOption(),
            createWebDavOption()
        )
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
            context.getString(R.string.external_storage),
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
        return FileDescriptor.fromRegularFile(Environment.getExternalStorageDirectory())
    }

    private fun createDropboxStorageDir(): FileDescriptor =
        FileDescriptor(
            fsAuthority = FSAuthority.DROPBOX_FS_AUTHORITY,
            path = ROOT_PATH,
            uid = ROOT_PATH,
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
            isDirectory = true,
            isRoot = true
        )

    suspend fun getRemoteFileSystemRoot(fsAuthority: FSAuthority): OperationResult<FileDescriptor> =
        withContext(Dispatchers.IO) {
            val provider = fileSystemResolver.resolveProvider(fsAuthority)
            provider.rootFile
        }
}