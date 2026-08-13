package com.ivanovsky.passnotes.presentation.enterDbCredentials

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.mapWithObject
import kotlinx.coroutines.withContext

class EnterDbCredentialsInteractor(
    private val dbRepository: EncryptedDatabaseRepository,
    private val fileSystemResolver: FileSystemResolver,
    private val settings: Settings,
    private val dispatchers: DispatcherProvider
) {

    suspend fun isValidKey(
        key: EncryptedDatabaseKey,
        file: FileDescriptor
    ): OperationResult<Unit> =
        withContext(dispatchers.IO) {
            dbRepository.read(settings.keepassImplementation, key, file, FSOptions.READ_ONLY)
                .mapWithObject(Unit)
        }

    suspend fun getFile(
        path: String,
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(fsAuthority)
                .getFile(path, FSOptions.READ_ONLY)
        }
}