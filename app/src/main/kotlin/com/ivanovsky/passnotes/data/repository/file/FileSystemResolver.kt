package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.repository.file.git.GitClient
import com.ivanovsky.passnotes.data.repository.file.git.GitFileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClientAdapter
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.saf.SAFFileSystemProvider
import com.ivanovsky.passnotes.data.repository.file.webdav.WebDavClientV2
import com.ivanovsky.passnotes.data.repository.file.webdav.WebdavAuthenticator
import com.ivanovsky.passnotes.injection.GlobalInjector.get
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class FileSystemResolver(
    protected open val factories: Map<FSType, Factory>
) {

    private val providers: MutableMap<FSAuthority, FileSystemProvider> = HashMap()
    private val lock = ReentrantLock()

    fun getAvailableFsTypes(): Set<FSType> {
        return factories.keys
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
        val factory = factories[fsAuthority.type]
            ?: throw IllegalStateException("No factory for type: ${fsAuthority.type}")

        return factory.createProvider(fsAuthority)
    }

    companion object {

        fun buildFactories(
            isExternalStorageAccessEnabled: Boolean
        ): Map<FSType, Factory> {
            val result = mutableMapOf(
                FSType.INTERNAL_STORAGE to InternalFileSystemFactory(),
                FSType.SAF to SAFFileSystemFactory(),
                FSType.WEBDAV to WebdavFileSystemFactory(),
                FSType.GIT to GitFileSystemFactory()
            )

            if (isExternalStorageAccessEnabled) {
                result[FSType.EXTERNAL_STORAGE] = ExternalFileSystemFactory()
            }

            return result
        }
    }

    fun interface Factory {
        fun createProvider(fsAuthority: FSAuthority): FileSystemProvider
    }

    class InternalFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            return RegularFileSystemProvider(get(), FSAuthority.INTERNAL_FS_AUTHORITY)
        }
    }

    class ExternalFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            return RegularFileSystemProvider(get(), FSAuthority.EXTERNAL_FS_AUTHORITY)
        }
    }

    class SAFFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            return SAFFileSystemProvider(get(), get())
        }
    }

    class WebdavFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            val authenticator = WebdavAuthenticator(fsAuthority)
            val client = RemoteApiClientAdapter(WebDavClientV2(authenticator))

            return RemoteFileSystemProvider(
                authenticator,
                client,
                get(),
                get(),
                get(),
                fsAuthority
            )
        }
    }

    class GitFileSystemFactory : Factory {
        override fun createProvider(fsAuthority: FSAuthority): FileSystemProvider {
            val authenticator = GitFileSystemAuthenticator(fsAuthority)
            val client = RemoteApiClientAdapter(
                GitClient(
                    get(),
                    authenticator,
                    get(),
                    get(),
                    get(),
                    get()
                )
            )

            return RemoteFileSystemProvider(
                authenticator,
                client,
                get(),
                get(),
                get(),
                fsAuthority
            )
        }
    }
}