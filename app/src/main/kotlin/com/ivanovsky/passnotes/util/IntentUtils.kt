package com.ivanovsky.passnotes.util

import android.content.Intent
import android.os.Build

object IntentUtils {

    fun createFilePickerIntent(): Intent {
        val action = if (Build.VERSION.SDK_INT >= 19) {
            Intent.ACTION_OPEN_DOCUMENT
        } else {
            Intent.ACTION_GET_CONTENT
        }

        return Intent(action).apply {
            type = "*/*";
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
}