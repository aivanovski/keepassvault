package com.ivanovsky.passnotes.presentation.notes

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class NotesPresenter(
    private val groupUid: UUID,
    private val view: NotesContract.View
) : NotesContract.Presenter {

    @Inject
    lateinit var interactor: NotesInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var resourceHelper: ResourceHelper

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun start() {
        loadData()
    }

    override fun destroy() {
        job.cancel()
    }

    override fun loadData() {
        scope.launch {
            val result = withContext(Dispatchers.Main) {
                interactor.getNotesByGroupUid(groupUid)
            }

            if (result.isSucceededOrDeferred) {
                val notes = result.obj

                if (notes.isNotEmpty()) {
                    view.showNotes(notes)
                    view.screenState = ScreenState.data()
                } else {
                    val emptyText = resourceHelper.getString(R.string.no_items)
                    view.screenState = ScreenState.empty(emptyText)
                }
            } else {
                val errorMessage = errorInteractor.processAndGetMessage(result.error)
                view.screenState = ScreenState.error(errorMessage)
            }
        }
    }
}