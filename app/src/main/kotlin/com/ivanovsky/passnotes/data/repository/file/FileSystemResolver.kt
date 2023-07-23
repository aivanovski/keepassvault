package com.ivanovsky.passnotes.data.repository.file

import android.content.Context
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.db.dao.GitRootDao
import com.ivanovsky.passnotes.data.repository.file.git.GitClient
import com.ivanovsky.passnotes.data.repository.file.git.GitFileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClientAdapter
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.saf.SAFFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.saf.SAFHelper
import com.ivanovsky.passnotes.data.repository.file.webdav.WebDavClientV2
import com.ivanovsky.passnotes.data.repository.file.webdav.WebdavAuthenticator
import com.ivanovsky.passnotes.domain.FileHelper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import okhttp3.OkHttpClient

class FileSystemResolver(
    private val context: Context,
    private val safHelper: SAFHelper,
    private val remoteFileRepository: RemoteFileRepository,
    private val gitRootDao: GitRootDao,
    private val fileHelper: FileHelper,
    private val observerBus: ObserverBus,
    private val httpClient: OkHttpClient,
    private val fsTypes: Set<FSType>,
    private val factories: Map<FSType, Factory> = emptyMap()
) {

    private val providers: MutableMap<FSAuthority, FileSystemProvider> = HashMap()
    private val lock = ReentrantLock()

    fun getAvailableFsTypes(): Set<FSType> {
        return fsTypes
    }

    fun resolveProvider(fsAuthority: FSAuthority): FileSystemProvider {
        var result: FileSystemProvider

        lock.withLock {
            val provider = providers[fsAuthority]
            if (provider == null) {
                result = instantiateProvider(fsAuthority).also {
                    providers[fsAuthority] = it
                }
            } else {
                result = provider
            }
        }

        return result
    }

    fun resolveSyncProcessor(fsAuthority: FSAuthority): FileSystemSyncProcessor {
        return resolveProvider(fsAuthority).syncProcessor
    }

    private fun instantiateProvider(fsAuthority: FSAuthority): FileSystemProvider {
        // Factory is used to instantiate FileSystemProvider for test usage
        val factory = factories[fsAuthority.type]
        if (factory != null) {
            return factory.createProvider(fsAuthority)
        }

        return when (fsAuthority.type) {
            FSType.INTERNAL_STORAGE -> {
                RegularFileSystemProvider(context, FSAuthority.INTERNAL_FS_AUTHORITY)
            }
            FSType.EXTERNAL_STORAGE -> {
                RegularFileSystemProvider(context, FSAuthority.EXTERNAL_FS_AUTHORITY)
            }
            FSType.WEBDAV -> {
                val authenticator = WebdavAuthenticator(fsAuthority)
                val client = RemoteApiClientAdapter(WebDavClientV2(authenticator, httpClient))
                RemoteFileSystemProvider(
                    authenticator,
                    client,
                    remoteFileRepository,
                    fileHelper,
                    observerBus,
                    fsAuthority
                )
            }
            FSType.SAF -> {
                SAFFileSystemProvider(context, safHelper)
            }
            FSType.GIT -> {
                val authenticator = GitFileSystemAuthenticator(fsAuthority)
                val client = RemoteApiClientAdapter(
                    GitClient(authenticator, fileHelper, gitRootDao)
                )

                RemoteFileSystemProvider(
                    authenticator,
                    client,
                    remoteFileRepository,
                    fileHelper,
                    observerBus,
                    fsAuthority
                )
            }
            else -> throw IllegalStateException("Invalid file system type: ${fsAuthority.type}")
        }
    }

    fun interface Factory {
        fun createProvider(fsAuthority: FSAuthority): FileSystemProvider
    }
}