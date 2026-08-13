package com.ivanovsky.passnotes.presentation.core.dialog.confirmationDialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.dialog.BaseDialogFragment
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class ConfirmationDialog : BaseDialogFragment(), DialogInterface.OnClickListener {

    var onConfirmed: ((actionId: Int) -> Unit)? = null
    var onDenied: ((actionId: Int) -> Unit)? = null
    var onNeutral: (() -> Unit)? = null

    private val args: ConfirmationDialogArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context, R.style.AppDialogTheme)
            .apply {
                setMessage(args.message)

                setPositiveButton(args.positiveButtonText, this@ConfirmationDialog)
                setNegativeButton(args.negativeButtonText, this@ConfirmationDialog)

                if (args.neutralButtonText != null) {
                    setNeutralButton(args.neutralButtonText, this@ConfirmationDialog)
                }
            }
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> onConfirmed?.invoke(args.actionId)
            DialogInterface.BUTTON_NEGATIVE -> onDenied?.invoke(args.actionId)
            DialogInterface.BUTTON_NEUTRAL -> onNeutral?.invoke()
        }
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        val TAG = ConfirmationDialog::class.qualifiedName

        fun newInstance(
            message: String,
            positiveButtonText: String,
            negativeButtonText: String,
            neutralButtonText: String? = null
        ): ConfirmationDialog {
            val args = ConfirmationDialogArgs(
                actionId = 0,
                message = message,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                neutralButtonText = neutralButtonText
            )

            return ConfirmationDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }

        fun newInstance(args: ConfirmationDialogArgs) =
            ConfirmationDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
    }
}