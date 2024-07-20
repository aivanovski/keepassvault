package com.ivanovsky.passnotes.domain.test.usecases

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.TestToggles
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.fake.DebugFileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.fake.FakeFileSystemProvider
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.listAllFiles
import com.ivanovsky.passnotes.extensions.mapError
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SetupFakeFileUseCase(
    private val fileSystemResolver: DebugFileSystemResolver,
    private val usedFileRepository: UsedFileRepository,
    private val settings: Settings,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: DispatcherProvider
) {

    fun setupFakeFile(fileName: String): OperationResult<String> {
        enableFakeFileSystem()
        setupFakeFileSystemFactory()
        return setupFile(fileName)
    }

    private fun enableFakeFileSystem() {
        settings.testToggles = (settings.testToggles ?: TestToggles())
            .copy(
                isFakeFileSystemEnabled = true
            )
    }

    private fun setupFakeFileSystemFactory() {
        val factories = FileSystemResolver.buildFactories(
            // TODO: value of isExternalStorageAccessEnabled should be resolved in runtime
            isExternalStorageAccessEnabled = true
        )
            .toMutableMap()
            .apply {
                this[FSType.FAKE] = DebugFileSystemResolver.FakeFileSystemFactory()
            }

        fileSystemResolver.setupFactories(factories)
    }

    private fun setupFile(fileName: String): OperationResult<String> {
        val fsProvider = fileSystemResolver.resolveProvider(FakeFileSystemProvider.FS_AUTHORITY)
        val getAllFiles = fsProvider.listAllFiles()
        if (getAllFiles.isFailed) {
            return getAllFiles.mapError()
        }

        val file = getAllFiles.getOrThrow()
            .firstOrNull { file -> file.name == fileName }
            ?: return OperationResult.error(newFileNotFoundError(fileName))

        val message = runBlocking {
            val existingUsedFile = withContext(dispatchers.IO) {
                usedFileRepository.findByUid(file.uid, file.fsAuthority)
            }

            if (existingUsedFile == null) {
                withContext(dispatchers.IO) {
                    usedFileRepository.insert(
                        UsedFile(
                            id = null,
                            fsAuthority = file.fsAuthority,
                            filePath = file.path,
                            fileUid = file.uid,
                            fileName = file.name,
                            isRoot = file.isRoot,
                            addedTime = System.currentTimeMillis(),
                            keyType = KeyType.PASSWORD
                        )
                    )
                }

                resourceProvider.getString(R.string.file_created_successfully)
            } else {
                resourceProvider.getString(R.string.file_already_exists)
            }
        }

        return OperationResult.success(message)
    }

    private fun newFileNotFoundError(fileName: String): OperationError {
        return OperationError.newFileNotFoundError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_FILE,
                fileName
            )
        )
    }
}