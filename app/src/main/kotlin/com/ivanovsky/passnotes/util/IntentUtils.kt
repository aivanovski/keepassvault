package com.ivanovsky.passnotes.util

import android.content.Intent

object IntentUtils {

    fun newOpenFileIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*";
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    fun newCreateFileIntent(filename: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, filename)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
}