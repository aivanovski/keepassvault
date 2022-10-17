package com.ivanovsky.passnotes.presentation.autofill

import android.content.Context
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.dialog.ConfirmationDialog

class AutofillDialogFactory(
    private val context: Context
) {

    fun createAddAutofillDataToNoteDialog(
        onConfirmed: () -> Unit,
        onDenied: () -> Unit
    ): DialogFragment {
        return ConfirmationDialog.newInstance(
            context.getString(R.string.add_autofill_data_message),
            context.getString(R.string.yes),
            context.getString(R.string.no)
        ).apply {
            this.onConfirmed = { onConfirmed.invoke() }
            this.onDenied = { onDenied.invoke() }
        }
    }
}