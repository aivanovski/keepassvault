package com.ivanovsky.passnotes.presentation.core.extensions

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.extensions.setItemVisibility
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.autofill.AutofillResponseFactory
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.dialog.ErrorDialog
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.util.InputMethodUtils

fun <T : Parcelable> Fragment.getMandatoryArgument(key: String): T {
    return arguments?.getParcelable(key) ?: requireArgument(key)
}

fun Fragment.getMandatoryStringArgument(key: String): String {
    return arguments?.getString(key) ?: requireArgument(key)
}

fun <T : Fragment> T.withArguments(initBlock: Bundle.() -> Unit): T {
    val args = Bundle()
    initBlock.invoke(args)
    arguments = args
    return this
}

fun Fragment.requireArgument(argumentName: String): Nothing {
    throw IllegalStateException("require argument with name: $argumentName")
}

fun Fragment.setupActionBar(action: ActionBar.() -> Unit) {
    val activity = (this.activity as? AppCompatActivity) ?: return

    activity.supportActionBar?.run {
        action.invoke(this)
    }
}

fun Fragment.hideKeyboard() {
    val activity = this.activity ?: return

    InputMethodUtils.hideSoftInput(activity)
}

fun Fragment.finishActivity() {
    activity?.finish()
}

fun Fragment.showToastMessage(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT)
        .show()
}

fun Fragment.showSnackbarMessage(message: String) {
    val view = findViewForSnackbar() ?: return

    Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        .show()
}

fun Fragment.sendAutofillResult(note: Note?, structure: AutofillStructure) {
    if (Build.VERSION.SDK_INT < 26) {
        return
    }

    val factory = AutofillResponseFactory(requireContext(), GlobalInjector.get())
    val response = if (note != null) {
        factory.createResponseWithNoteAndSelection(note, structure)
    } else {
        factory.createResponseWithSelection(structure)
    }

    val result = Intent()
        .apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
        }

    requireActivity().setResult(Activity.RESULT_OK, result)
}

fun Fragment.showErrorDialog(message: String) {
    val dialog = ErrorDialog.newInstance(message)
    dialog.show(childFragmentManager, ErrorDialog.TAG)
}

fun Fragment.updateMenuItemVisibility(
    menu: Menu,
    visibleItems: List<ScreenMenuItem>,
    allScreenItems: List<ScreenMenuItem>
) {
    allScreenItems.forEach { item ->
        menu.setItemVisibility(
            id = item.menuId,
            isVisible = visibleItems.contains(item)
        )
    }
}

private fun Fragment.findViewForSnackbar(): View? {
    return view?.findViewById(R.id.rootLayout)
}
