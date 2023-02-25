package com.ivanovsky.passnotes.domain.interactor.serverLogin

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.usecases.test.GetTestCredentialsUseCase
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import kotlinx.coroutines.withContext

class ServerLoginInteractor(
    private val dispatchers: DispatcherProvider,
    private val fileSystemResolver: FileSystemResolver,
    private val getTestCredentialsUseCase: GetTestCredentialsUseCase
) {

    fun getTestWebDavCredentials() = getTestCredentialsUseCase.getDebugWebDavCredentials()

    fun getTestGitCredentials() = getTestCredentialsUseCase.getDebugGitCredentials()

    suspend fun authenticate(
        credentials: FSCredentials,
        fsAuthority: FSAuthority
    ): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            val fileSystemProvider = fileSystemResolver.resolveProvider(fsAuthority)
            val authenticator = fileSystemProvider.authenticator
            authenticator.setCredentials(credentials)

            val getRootResult = fileSystemProvider.rootFile
            if (getRootResult.isFailed) {
                authenticator.setCredentials(null)
                return@withContext getRootResult.mapError()
            }

            getRootResult.mapWithObject(Unit)
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