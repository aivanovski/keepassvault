package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R

class ConfirmationDialog : DialogFragment(), DialogInterface.OnClickListener {

    var onConfirmed: (() -> Unit)? = null
    var onDenied: (() -> Unit)? = null
    var onNeutral: (() -> Unit)? = null
    private var message: String? = null
    private var positiveButtonText: String? = null
    private var negativeButtonText: String? = null
    private var neutralButtonText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context, R.style.AppDialogTheme)
            .apply {
                setMessage(message)

                setPositiveButton(positiveButtonText, this@ConfirmationDialog)
                setNegativeButton(negativeButtonText, this@ConfirmationDialog)

                if (neutralButtonText != null) {
                    setNeutralButton(neutralButtonText, this@ConfirmationDialog)
                }
            }
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> onConfirmed?.invoke()
            DialogInterface.BUTTON_NEGATIVE -> onDenied?.invoke()
            DialogInterface.BUTTON_NEUTRAL -> onNeutral?.invoke()
        }
    }

    companion object {

        val TAG = ConfirmationDialog::class.qualifiedName

        fun newInstance(
            message: String,
            positiveButtonText: String,
            negativeButtonText: String,
            neutralButtonText: String? = null
        ): ConfirmationDialog {
            return ConfirmationDialog()
                .apply {
                    this.message = message
                    this.positiveButtonText = positiveButtonText
                    this.negativeButtonText = negativeButtonText
                    this.neutralButtonText = neutralButtonText
                }
        }
    }
}