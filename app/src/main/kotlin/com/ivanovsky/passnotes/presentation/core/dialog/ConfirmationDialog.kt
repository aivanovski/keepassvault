package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kotlin.reflect.jvm.jvmName

class ConfirmationDialog : DialogFragment(), DialogInterface.OnClickListener {

    lateinit var onConfirmationLister: () -> Unit
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
            .setNegativeButton(negativeButtonText, null)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        onConfirmationLister.invoke()
    }

    companion object {

        val TAG = ConfirmationDialog::class.jvmName

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