package com.ivanovsky.passnotes.presentation.core.extensions

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.SnackbarMessage
import com.ivanovsky.passnotes.util.InputMethodUtils

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

fun Fragment.showSnackbar(message: SnackbarMessage) {
    val view = findViewForSnackbar() ?: return

    val snackbar = if (message.isDisplayOkButton) {
        val snack = Snackbar.make(view, message.message, Snackbar.LENGTH_INDEFINITE)
        snack.setAction(R.string.ok) { snack.dismiss() }
        snack
    } else {
        Snackbar.make(view, message.message, Snackbar.LENGTH_SHORT)
    }

    snackbar.show()
}

private fun Fragment.findViewForSnackbar(): View? {
    return view?.findViewById(R.id.rootLayout)
}