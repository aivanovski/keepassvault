package com.ivanovsky.passnotes.domain.interactor.note

import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ClipboardInteractor
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.usecases.CheckNoteAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.GetLastSyncStatusUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.util.InputOutputUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.withContext

class NoteInteractor(
    private val clipboardInteractor: ClipboardInteractor,
    private val lockUseCase: LockDatabaseUseCase,
    private val getLastSyncStatusUseCase: GetLastSyncStatusUseCase,
    private val getDbUseCase: GetDatabaseUseCase,
    private val autofillUseCase: UpdateNoteWithAutofillDataUseCase,
    private val checkNoteAutofillDataUseCase: CheckNoteAutofillDataUseCase,
    private val settings: Settings,
    private val fileHelper: FileHelper,
    private val dispatchers: DispatcherProvider
) {

    suspend fun saveAttachmentToStorage(attachment: Attachment): OperationResult<File> =
        withContext(dispatchers.IO) {
            val generateDirResult = fileHelper.generateDestinationDirectoryForSharedFile()
            if (generateDirResult.isFailed) {
                return@withContext generateDirResult.mapError()
            }

            val outDir = generateDirResult.getOrThrow()
            val outFile = File(outDir, attachment.name)

            val source = ByteArrayInputStream(attachment.data)
            val copyResult = InputOutputUtils.copy(source, outFile)
            if (copyResult.isFailed) {
                return@withContext copyResult.mapError()
            }

            OperationResult.success(outFile)
        }

    suspend fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabaseSynchronously()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val db = getDbResult.obj
            db.noteDao.getNoteByUid(noteUid)
        }

    fun copyToClipboardWithTimeout(text: String, isProtected: Boolean) {
        clipboardInteractor.copyWithTimeout(text, isProtected, getTimeoutValueInMillis())
    }

    fun copyToClipboard(text: String, isProtected: Boolean) {
        clipboardInteractor.copy(text, isProtected)
    }

    fun getTimeoutValueInMillis(): Long {
        return settings.autoClearClipboardDelayInMs.toLong()
    }

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }

    suspend fun getLastSyncStatus(): OperationResult<SyncStatus?> =
        getLastSyncStatusUseCase.getLastSyncStatus()

    suspend fun updateNoteWithAutofillData(
        note: Note,
        structure: AutofillStructure
    ): OperationResult<Boolean> =
        autofillUseCase.updateNoteWithAutofillData(note, structure)

    suspend fun shouldUpdateNoteAutofillData(
        note: Note,
        structure: AutofillStructure
    ): Boolean =
        checkNoteAutofillDataUseCase.shouldUpdateNoteAutofillData(note, structure)
}