package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class MessageDialog : DialogFragment() {

    private val args: MessageDialogArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext(), R.style.AppDialogTheme)
            .apply {
                if (args.isError) {
                    setTitle(R.string.error_has_been_occurred)
                }
                setMessage(args.message)
                if (!args.isError) {
                    setNeutralButton(R.string.ok, null)
                }
            }
            .create()
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        val TAG = MessageDialog::class.simpleName

        fun newErrorDialogInstance(
            message: String
        ): MessageDialog {
            val args = MessageDialogArgs(
                isError = false,
                message = message
            )

            return MessageDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }

        fun newMessageDialogInstance(
            message: String
        ): MessageDialog {
            val args = MessageDialogArgs(
                isError = false,
                message = message
            )

            return MessageDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}