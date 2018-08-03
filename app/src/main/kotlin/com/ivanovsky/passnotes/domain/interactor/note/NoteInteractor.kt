package com.ivanovsky.passnotes.domain.interactor.note

import android.os.Handler
import android.os.Looper
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.NoteRepository
import com.ivanovsky.passnotes.domain.ClipboardHelper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class NoteInteractor(private val noteRepository: NoteRepository,
                     private val clipboardHelper: ClipboardHelper) {

	fun getNoteByUid(noteUid: UUID): Single<OperationResult<Note>> {
		return Single.fromCallable { noteRepository.getNoteByUid(noteUid) }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
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