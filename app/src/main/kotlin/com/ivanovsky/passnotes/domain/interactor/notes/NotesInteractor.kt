package com.ivanovsky.passnotes.domain.interactor.notes

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.NoteRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class NotesInteractor(private val noteRepository: NoteRepository) {

	fun getNotesByGroupUid(groupUid: UUID): Single<OperationResult<List<Note>>> {
		return Single.fromCallable { noteRepository.getNotesByGroupUid(groupUid) }
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
	}
}