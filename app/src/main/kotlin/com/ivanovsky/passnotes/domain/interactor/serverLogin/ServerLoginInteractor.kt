package com.ivanovsky.passnotes.domain.interactor.serverLogin

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.test.GetTestCredentialsUseCase
import com.ivanovsky.passnotes.extensions.mapError
import kotlinx.coroutines.withContext

class ServerLoginInteractor(
    private val dispatchers: DispatcherProvider,
    private val fileSystemResolver: FileSystemResolver,
    private val getTestCredentialsUseCase: GetTestCredentialsUseCase
) {

    fun getTestWebDavCredentials() = getTestCredentialsUseCase.getDebugWebDavCredentials()

    fun getTestGitCredentials() = getTestCredentialsUseCase.getDebugGitCredentials()

    fun getTestFakeCredentials() = getTestCredentialsUseCase.getDebugFakeCredentials()

    suspend fun authenticate(
        credentials: FSCredentials,
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            val getRootResult = tryAuthenticate(credentials, fsAuthority)
            if (getRootResult.isSucceeded) {
                return@withContext getRootResult
            }

            val notBrowsableFSAuthority = fsAuthority.copy(
                isBrowsable = false
            )
            tryAuthenticate(credentials, notBrowsableFSAuthority)
        }

    private suspend fun tryAuthenticate(
        credentials: FSCredentials,
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            val fileSystemProvider = fileSystemResolver.resolveProvider(fsAuthority)
            fileSystemProvider.authenticator.setCredentials(credentials)

            val result = fileSystemProvider.rootFile
            if (result.isFailed) {
                fileSystemProvider.authenticator.setCredentials(null)
                return@withContext result.mapError()
            }

            result
        }

    suspend fun saveCredentials(
        credentials: FSCredentials,
        fsAuthority: FSAuthority
    ): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val fileSystemProvider = fileSystemResolver.resolveProvider(fsAuthority)

            fileSystemProvider.authenticator.apply {
                setCredentials(credentials)
            }

            OperationResult.success(Unit)
        }
}