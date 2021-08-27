package com.ivanovsky.passnotes.domain.interactor.note

import android.os.Handler
import android.os.Looper
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ClipboardHelper
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseStatusUseCase
import java.util.UUID

class NoteInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val clipboardHelper: ClipboardHelper,
    private val lockUseCase: LockDatabaseUseCase,
    private val getStatusUseCase: GetDatabaseStatusUseCase,
    private val settings: Settings
) {

    fun getNoteByUid(noteUid: UUID): OperationResult<Note> {
        return dbRepo.noteRepository.getNoteByUid(noteUid)
    }

    fun copyToClipboardWithTimeout(text: String) {
        clipboardHelper.copy(text)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ clipboardHelper.clear() }, getTimeoutValueInMillis())
    }

    fun getTimeoutValueInMillis(): Long {
        return settings.autoClearClipboardDelayInMs.toLong()
    }

    fun lockDatabase() {
        lockUseCase.lockIfNeed()
    }

    suspend fun getDatabaseStatus(): OperationResult<DatabaseStatus> =
        getStatusUseCase.getDatabaseStatus()
}