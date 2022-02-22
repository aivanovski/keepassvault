package com.ivanovsky.passnotes.presentation.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.annotation.RequiresApi
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.interactor.autofill.AutofillInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector.get
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(26)
class PassnotesAutofillService : AutofillService() {

    private val interactor: AutofillInteractor by inject()
    private val responseFactory = AutofillResponseFactory(this, get())
    private val dispatchers: DispatcherProvider by inject()

    private val job = Job()
    private val scope = CoroutineScope(dispatchers.Main + job)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        Logger.d(TAG, "onFillRequest:")

        val fillContexts = request.fillContexts

        val latestStructure = fillContexts.lastOrNull()?.structure
        if (latestStructure == null) {
            callback.onSuccess(null)
            return
        }

        val structure = AutofillStructureParser().parse(latestStructure)
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        if (!structure.hasFieldsToFill()) {
            callback.onSuccess(null)
            return
        }

        if (!interactor.isDatabaseOpened()) {
            val response = responseFactory.createResponseWithUnlock(structure)
            callback.onSuccess(response)
            return
        }

        scope.launch {
            val findNoteResult = interactor.findNoteForAutofill(structure)
            if (findNoteResult.isSucceeded) {
                val note = findNoteResult.obj.second

                if (note != null) {
                    Logger.d(TAG, "Show note and selection")
                    val response = responseFactory.createResponseWithNoteAndSelection(note, structure)
                    callback.onSuccess(response)
                } else {
                    Logger.d(TAG, "Show selection")
                    val response = responseFactory.createResponseWithSelection(structure)
                    callback.onSuccess(response)
                }
            } else {
                Logger.d(TAG, "Error has occurred, nothing to show")
                callback.onSuccess(null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "onDestroy:")
        job.cancel()
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Logger.d(TAG, "onSaveRequest:")
    }

    companion object {
        private val TAG = PassnotesAutofillService::class.java.simpleName
    }
}