package com.ivanovsky.passnotes.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object IntentUtils {

    fun newViewFileIntent(
        context: Context,
        file: File,
        mimeType: String? = null
    ): Intent {
        val uri = FileProvider.getUriForFile(context, context.packageName, file)
        val mime = if (mimeType == null) {
            FileUtils.getMimeTypeFromName(file.name)
        } else {
            null
        }

        return Intent()
            .apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(uri, mime ?: FileUtils.MIME_TYPE_TEXT)
            }
    }

    fun newShareFileIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, context.packageName, file)
        val mimeType = FileUtils.getMimeTypeFromName(file.name) ?: FileUtils.MIME_TYPE_TEXT

        return Intent()
            .apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = mimeType
            }
    }

    fun newOpenUrlIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
    }

    fun newOpenFileIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    fun newCreateFileIntent(filename: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, filename)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    fun defaultPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    fun immutablePendingIntentFlags(vararg flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_IMMUTABLE + flags.sum()
        } else {
            flags.sum()
        }
    }
}