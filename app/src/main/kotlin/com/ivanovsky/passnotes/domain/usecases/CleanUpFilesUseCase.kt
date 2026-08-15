package com.ivanovsky.passnotes.domain.usecases

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.TemporaryFile
import com.ivanovsky.passnotes.data.repository.TemporaryFileRepository
import java.io.File
import java.util.concurrent.TimeUnit
import timber.log.Timber

class CleanUpFilesUseCase(
    private val temporaryFileRepo: TemporaryFileRepository
) {

    // TODO: cleanup RemoteFile

    fun removeUnusedFiles(): Either<OperationError, Unit> =
        either {
            val allTemporaryFiles = temporaryFileRepo.getAll()

            Timber.d("Remove unused files: files.size=%s".format(allTemporaryFiles.size))

            for (file in allTemporaryFiles) {
                val osFile = File(file.path)

                val isExist = osFile.exists()
                val isTooOld = file.isTooOld()

                if (!isExist || isTooOld) {
                    Timber.d(
                        "Removing file: file=%s, isExist=%s, isOld=%s".format(
                            file.path,
                            isExist,
                            isTooOld
                        )
                    )

                    temporaryFileRepo.removeByPath(file.path)
                    if (isExist) {
                        osFile.delete()
                    }
                }
            }
        }

    private fun TemporaryFile.isTooOld(): Boolean {
        return created >= System.currentTimeMillis() - ONE_WEEK_IN_MILLIS
    }

    companion object {
        private val ONE_WEEK_IN_MILLIS = TimeUnit.DAYS.toMillis(7)
    }
}