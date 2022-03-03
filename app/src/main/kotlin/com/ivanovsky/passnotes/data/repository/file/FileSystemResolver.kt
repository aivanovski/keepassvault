package com.ivanovsky.passnotes.data.repository.file

import android.content.Context
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.dropbox.DropboxAuthenticator
import com.ivanovsky.passnotes.data.repository.file.dropbox.DropboxClient
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClientAdapter
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.saf.SAFFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.webdav.WebDavClientV2
import com.ivanovsky.passnotes.data.repository.file.webdav.WebdavAuthenticator
import com.ivanovsky.passnotes.data.repository.settings.Settings
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import okhttp3.OkHttpClient

class FileSystemResolver(
    private val context: Context,
    private val settings: Settings,
    private val remoteFileRepository: RemoteFileRepository,
    private val fileHelper: FileHelper,
    private val observerBus: ObserverBus,
    private val httpClient: OkHttpClient
) {

    private val providers: MutableMap<FSAuthority, FileSystemProvider> = HashMap()
    private val lock = ReentrantLock()

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
        return when (fsAuthority.type) {
            FSType.REGULAR_FS -> {
                RegularFileSystemProvider()
            }
            FSType.DROPBOX -> {
                val authenticator = DropboxAuthenticator(settings)
                RemoteFileSystemProvider(
                    authenticator,
                    DropboxClient(authenticator),
                    remoteFileRepository,
                    fileHelper,
                    observerBus,
                    fsAuthority
                )
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
                SAFFileSystemProvider(context)
            }
        }
    }
}