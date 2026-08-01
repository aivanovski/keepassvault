package com.ivanovsky.passnotes.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object InputMethodUtils {
    @JvmStatic
    fun hideSoftInput(activity: Activity?) {
        val focusedView = activity?.window?.currentFocus ?: return
        val windowToken = focusedView.windowToken ?: return
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
    }

    @JvmStatic
    fun showSoftInput(context: Context?, view: View?) {
        if (context == null || view == null) return

        view.requestFocus()
        view.postDelayed(
            {
                val inputMethodManager =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            },
            100
        )
    }
}