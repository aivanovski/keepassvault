package com.ivanovsky.passnotes.presentation.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.annotation.RequiresApi
import com.ivanovsky.passnotes.data.entity.NoteCandidate
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.PropertyFactory
import com.ivanovsky.passnotes.domain.PropertyFactory.createPasswordProperty
import com.ivanovsky.passnotes.domain.PropertyFactory.createUsernameProperty
import com.ivanovsky.passnotes.domain.interactor.autofill.AutofillInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector.get
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.main.MainActivity
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

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
        Timber.d("onFillRequest:")

        val latestStructure = request.fillContexts.lastOrNull()?.structure
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
                val note = findNoteResult.obj

                if (note != null) {
                    Timber.d("Show note and selection")
                    val response = responseFactory.createResponseWithNoteAndSelection(note, structure)
                    callback.onSuccess(response)
                } else {
                    Timber.d("Show selection")
                    val response = responseFactory.createResponseWithSelection(structure)
                    callback.onSuccess(response)
                }
            } else {
                Timber.d("Error has occurred, nothing to show")
                callback.onSuccess(null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy:")
        job.cancel()
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Timber.d("onSaveRequest:")

        val latestStructure = request.fillContexts.lastOrNull()?.structure
        if (latestStructure == null) {
            callback.onSuccess()
            return
        }

        val structure = AutofillStructureParser().parse(latestStructure)
        if (structure == null) {
            callback.onSuccess()
            return
        }

        val note = createNote(structure)
        if (Build.VERSION.SDK_INT >= 28) {
            val intent = MainActivity.createAutofillSaveResultPendingIntent(this, note)
            callback.onSuccess(intent.intentSender)
        } else {
            val intent = MainActivity.createAutofillSaveResultIntent(this, note)
            callback.onSuccess()
            startActivity(intent)
        }
    }

    private fun createNote(structure: AutofillStructure): NoteCandidate {
        val properties = mutableListOf(
            createUsernameProperty(
                structure.username?.autofillValue?.textValue?.toString() ?: EMPTY
            ),
            createPasswordProperty(
                structure.password?.autofillValue?.textValue?.toString() ?: EMPTY
            )
        )

        if (!structure.applicationId.isNullOrEmpty()) {
            properties.add(
                PropertyFactory.createAutofillAppIdProperty(structure.applicationId)
            )
        }

        if (!structure.webDomain.isNullOrEmpty()) {
            properties.add(
                PropertyFactory.createUrlProperty(structure.webDomain)
            )
        }

        return NoteCandidate(
            properties = properties
        )
    }

    companion object {
        private val TAG = PassnotesAutofillService::class.java.simpleName
    }
}