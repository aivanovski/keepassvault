package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ConfirmationDialog : DialogFragment(), DialogInterface.OnClickListener {

    var onConfirmed: (() -> Unit)? = null
    var onDenied: (() -> Unit)? = null
    private lateinit var message: String
    private lateinit var positiveButtonText: String
    private lateinit var negativeButtonText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton(positiveButtonText, this)
            .setNegativeButton(negativeButtonText, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                onConfirmed?.invoke()
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                onDenied?.invoke()
            }
        }
    }

    companion object {

        val TAG = ConfirmationDialog::class.qualifiedName

        fun newInstance(
            message: String,
            positiveButtonText: String,
            negativeButtonText: String
        ): ConfirmationDialog {
            val dialog = ConfirmationDialog()
            dialog.message = message
            dialog.positiveButtonText = positiveButtonText
            dialog.negativeButtonText = negativeButtonText
            return dialog
        }
    }
}