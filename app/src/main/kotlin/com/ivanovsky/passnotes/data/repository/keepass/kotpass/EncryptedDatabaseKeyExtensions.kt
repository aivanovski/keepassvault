package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.FileKeepassKey
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import java.lang.Exception
import timber.log.Timber

fun EncryptedDatabaseKey.toCredentials(
    fileSystemResolver: FileSystemResolver
): OperationResult<Credentials> {
    return when (this) {
        is PasswordKeepassKey -> {
            val credentials = Credentials.from(password.toByteArray())
            OperationResult.success(credentials)
        }

        is FileKeepassKey -> {
            val provider = fileSystemResolver.resolveProvider(file.fsAuthority)

            val inputResult = provider.openFileForRead(
                file,
                OnConflictStrategy.CANCEL,
                FSOptions.READ_ONLY
            )
            if (inputResult.isFailed) {
                return inputResult.takeError()
            }

            try {
                val input = inputResult.obj
                val bytes = input.use {
                    input.readBytes()
                }

                val credentials = if (password == null) {
                    Credentials.from(
                        keyData = bytes
                    )
                } else {
                    Credentials.from(
                        passphrase = EncryptedValue.fromString(password),
                        keyData = bytes
                    )
                }

                OperationResult.success(credentials)
            } catch (exception: Exception) {
                Timber.d(exception)
                OperationResult.error(
                    OperationError.newGenericIOError(
                        OperationError.MESSAGE_FAILED_TO_READ_KEY_FILE,
                        exception
                    )
                )
            }
        }

        else -> throw IllegalStateException()
    }
}