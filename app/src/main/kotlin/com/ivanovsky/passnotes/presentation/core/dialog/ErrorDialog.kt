package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.extensions.getMandarotyArgument
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class ErrorDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_was_occurred)
            .setMessage(getMandarotyArgument(ARG_MESSAGE))
            .create()
    }

    companion object {

        private const val ARG_MESSAGE = "message"

        val TAG = ErrorDialog::class.simpleName

        fun newInstance(message: String) = ErrorDialog().withArguments {
            putString(ARG_MESSAGE, message)
        }
    }
}