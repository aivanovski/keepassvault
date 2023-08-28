package com.ivanovsky.passnotes.presentation.core.dialog.helpDialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.extensions.cloneInContext
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class HelpDialog : DialogFragment() {

    private val args: HelpDialogArgs by lazy { getMandatoryArgument(ARGUMENTS) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val themedInflater = layoutInflater.cloneInContext(R.style.AppDialogTheme)
        val view = themedInflater.inflate(args.layoutId, null, false)

        return AlertDialog.Builder(requireContext(), R.style.AppDialogTheme)
            .apply {
                if (args.title != null) {
                    setTitle(args.title)
                }
                setView(view)
                setPositiveButton(R.string.ok, null)
            }
            .create()
    }

    companion object {

        val TAG = HelpDialog::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(arguments: HelpDialogArgs): HelpDialog {
            return HelpDialog()
                .withArguments {
                    putParcelable(ARGUMENTS, arguments)
                }
        }
    }
}