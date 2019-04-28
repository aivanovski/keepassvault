package com.ivanovsky.passnotes.domain.interactor.note

import android.os.Handler
import android.os.Looper
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.NoteRepository
import com.ivanovsky.passnotes.domain.ClipboardHelper
import java.util.*
import java.util.concurrent.TimeUnit

class NoteInteractor(private val noteRepository: NoteRepository,
                     private val clipboardHelper: ClipboardHelper) {

	fun getNoteByUid(noteUid: UUID): OperationResult<Note> {
		return noteRepository.getNoteByUid(noteUid)
	}

	fun copyToClipboardWithTimeout(text: String) {
		clipboardHelper.copy(text)

		val handler = Handler(Looper.getMainLooper())
		handler.postDelayed({ clipboardHelper.clear() }, TimeUnit.SECONDS.toMillis(30))
	}

	fun getTimeoutValueInMillis(): Long {
		return TimeUnit.SECONDS.toMillis(30)
	}
}