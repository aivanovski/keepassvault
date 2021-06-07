package com.ivanovsky.passnotes.domain.usecases

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.toFileDescriptor
import kotlinx.coroutines.withContext

class GetRecentlyOpenedFilesUseCase(
    private val fileRepository: UsedFileRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getRecentlyOpenedFiles(): OperationResult<List<FileDescriptor>> =
        withContext(dispatchers.IO) {
            val files = fileRepository.getAll()
                .sortedByDescending { file -> file.lastAccessTime ?: file.addedTime }
                .map { file -> file.toFileDescriptor() }

            OperationResult.success(files)
        }
}