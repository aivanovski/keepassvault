package com.ivanovsky.passnotes.domain.interactor.note

import android.os.Handler
import android.os.Looper
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.ClipboardHelper
import com.ivanovsky.passnotes.domain.usecases.DatabaseLockUseCase
import java.util.*
import java.util.concurrent.TimeUnit

class NoteInteractor(
    private val dbRepo: EncryptedDatabaseRepository,
    private val clipboardHelper: ClipboardHelper,
    private val lockUseCase: DatabaseLockUseCase
) {

    fun getNoteByUid(noteUid: UUID): OperationResult<Note> {
        return dbRepo.noteRepository.getNoteByUid(noteUid)
    }

    fun copyToClipboardWithTimeout(text: String) {
        clipboardHelper.copy(text)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ clipboardHelper.clear() }, TimeUnit.SECONDS.toMillis(30))
    }

    fun getTimeoutValueInMillis(): Long {
        return TimeUnit.SECONDS.toMillis(30)
    }

    fun closeDatabase() {
        lockUseCase.lockIfNeed()
    }
}