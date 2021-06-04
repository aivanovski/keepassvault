package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class ThreeButtonDialog : DialogFragment(), DialogInterface.OnClickListener {

    lateinit var onPositiveClicked: () -> Unit
    lateinit var onNegativeClicked: () -> Unit
    private lateinit var title: String
    private lateinit var message: String
    private lateinit var positiveButtonText: String
    private lateinit var negativeButtonText: String
    private lateinit var neutralButtonText: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
            .setPositiveButton(positiveButtonText, this)
            .setNegativeButton(negativeButtonText, this)
            .setNeutralButton(neutralButtonText, this)

        if (title.isNotEmpty()) {
            builder.setTitle(title)
        }

        if (message.isNotEmpty()) {
            builder.setMessage(message)
        }

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> onPositiveClicked.invoke()
            DialogInterface.BUTTON_NEGATIVE -> onNegativeClicked.invoke()
        }
    }

    companion object {

        val TAG = ThreeButtonDialog::class.qualifiedName

        fun newInstance(
            title: String = EMPTY,
            message: String = EMPTY,
            positiveButtonText: String,
            negativeButtonText: String,
            neutralButtonText: String
        ) = ThreeButtonDialog().apply {
            this.title = title
            this.message = message
            this.positiveButtonText = positiveButtonText
            this.negativeButtonText = negativeButtonText
            this.neutralButtonText = neutralButtonText
        }
    }
}