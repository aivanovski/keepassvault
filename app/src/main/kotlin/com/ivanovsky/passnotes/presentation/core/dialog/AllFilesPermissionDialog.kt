package com.ivanovsky.passnotes.presentation.core.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.injection.GlobalInjector.inject

class AllFilesPermissionDialog : DialogFragment(), DialogInterface.OnClickListener {

    lateinit var onPositiveClicked: () -> Unit
    lateinit var onNegativeClicked: () -> Unit

    private val permissionHelper: PermissionHelper by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context)
            .setTitle(R.string.all_files_permission_dialog_title)
            .setMessage(R.string.all_files_permission_dialog_message)
            .setPositiveButton(R.string.grant, this)
            .setNegativeButton(R.string.cancel, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> onPositiveClicked.invoke()
            DialogInterface.BUTTON_NEGATIVE -> onNegativeClicked.invoke()
        }
    }

    override fun onStart() {
        super.onStart()
        if (permissionHelper.isAllFilesPermissionGranted()) {
            dismiss()
        }
    }

    companion object {

        val TAG = AllFilesPermissionDialog::class.qualifiedName

        fun newInstance(): AllFilesPermissionDialog = AllFilesPermissionDialog()
    }
}