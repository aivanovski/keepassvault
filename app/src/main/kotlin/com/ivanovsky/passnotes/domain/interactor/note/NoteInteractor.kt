package com.ivanovsky.passnotes.domain.interactor.note

import android.os.Handler
import android.os.Looper
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ClipboardHelper
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.usecases.CheckNoteAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseStatusUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteInteractor(
    private val clipboardHelper: ClipboardHelper,
    private val lockUseCase: LockDatabaseUseCase,
    private val getStatusUseCase: GetDatabaseStatusUseCase,
    private val getDbUseCase: GetDatabaseUseCase,
    private val autofillUseCase: UpdateNoteWithAutofillDataUseCase,
    private val checkNoteAutofillDataUseCase: CheckNoteAutofillDataUseCase,
    private val settings: Settings,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabaseSynchronously()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.takeError()
            }

            val db = getDbResult.obj
            db.noteDao.getNoteByUid(noteUid)
        }

    fun copyToClipboardWithTimeout(text: String) {
        clipboardHelper.copy(text)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ clipboardHelper.clear() }, getTimeoutValueInMillis())
    }

    fun copyToClipboard(text: String) {
        clipboardHelper.copy(text)
    }

    fun getTimeoutValueInMillis(): Long {
        return settings.autoClearClipboardDelayInMs.toLong()
    }

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }

    suspend fun getDatabaseStatus(): OperationResult<DatabaseStatus> =
        getStatusUseCase.getDatabaseStatus()

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