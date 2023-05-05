package com.ivanovsky.passnotes.data.repository.file.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import timber.log.Timber

/**
 * Helper class for Storage-Access-Framework
 */
class SAFHelper(context: Context) {

    private val contentResolver = context.contentResolver

    fun setupPermissionIfNeed(uri: Uri): OperationResult<Unit> {
        val hasPersistedPermission = hasPersistablePermission(uri)

        Timber.d(
            "setupPermissionIfNeed: uri=%s, hasPermission=%s",
            uri,
            hasPersistedPermission
        )

        return if (!hasPersistedPermission) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                Timber.d(
                    "setupPermissionIfNeed: permission is set up successfully, uri=%s",
                    uri
                )

                OperationResult.success(Unit)
            } catch (exception: SecurityException) {
                Timber.d(exception)
                OperationResult.error(failedToGetAccessTo(uri))
            } catch (exception: Exception) {
                Timber.d(exception)
                OperationResult.error(
                    newGenericIOError(
                        OperationError.MESSAGE_UNKNOWN_ERROR,
                        exception
                    )
                )
            }
        } else {
            OperationResult.success(Unit)
        }
    }

    private fun failedToGetAccessTo(uri: Uri): OperationError {
        return newFileAccessError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_GET_ACCESS_RIGHT_TO_URI,
                uri.toString()
            )
        )
    }

    private fun hasPersistablePermission(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any { persistedUri ->
            persistedUri.uri == uri &&
                    persistedUri.isReadPermission &&
                    persistedUri.isWritePermission
        }
    }
}