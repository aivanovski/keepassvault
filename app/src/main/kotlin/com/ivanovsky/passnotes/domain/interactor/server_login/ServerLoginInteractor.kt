package com.ivanovsky.passnotes.domain.interactor.server_login

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.DispatcherProvider
import kotlinx.coroutines.withContext

class ServerLoginInteractor(
    private val dispatchers: DispatcherProvider,
    private val fileSystemResolver: FileSystemResolver,
    private val getDebugCredentialsUseCase: GetDebugCredentialsUseCase
) {

    fun getDebugWebDavCredentials() = getDebugCredentialsUseCase.getDebugWebDavCredentials()

    suspend fun authenticate(
        credentials: ServerCredentials,
        fsAuthority: FSAuthority
    ): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val fileSystemProvider = fileSystemResolver.resolveProvider(fsAuthority)
            val authenticator = fileSystemProvider.authenticator
            authenticator.setCredentials(credentials)

            val root = fileSystemProvider.rootFile

            if (root.isSucceeded) {
                OperationResult.success(true)
            } else {
                authenticator.setCredentials(null)
                root.takeError()
            }
        }

    suspend fun saveCredentials(
        credentials: ServerCredentials,
        fsAuthority: FSAuthority
    ): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val fileSystemProvider = fileSystemResolver.resolveProvider(fsAuthority)

            fileSystemProvider.authenticator.apply {
                setCredentials(credentials)
            }

            OperationResult.success(true)
        }
}