package com.ivanovsky.passnotes.domain.usecases

import android.net.Uri
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.saf.SAFHelper
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import kotlinx.coroutines.withContext

class ExportDatabaseUseCase(
    private val fsResolver: FileSystemResolver,
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider,
    private val safHelper: SAFHelper
) {

    suspend fun exportDatabase(destination: Uri): OperationResult<Boolean> =
        withContext(dispatchers.IO) {
            val setupPermissionsResult = safHelper.setupPermissionIfNeed(destination)
            if (setupPermissionsResult.isFailed) {
                return@withContext setupPermissionsResult.mapError()
            }

            val getDbResult = getDbUseCase.getDatabaseSynchronously()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val db = getDbResult.getOrThrow()

            val provider = fsResolver.resolveProvider(FSAuthority.SAF_FS_AUTHORITY)
            val getFileResult = provider.getFile(destination.toString(), FSOptions.DEFAULT)
            if (getFileResult.isFailed) {
                return@withContext getFileResult.mapError()
            }

            val file = getFileResult.getOrThrow()
            db.commitTo(file, FSOptions.DEFAULT)
        }
}