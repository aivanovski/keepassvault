package com.ivanovsky.passnotes.domain

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PersistableBundle
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class ClipboardInteractor(context: Context) {

    private val isDirty = AtomicBoolean(false)
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val clearHandler = ClipboardClearHandler(this)

    fun copy(text: String, isProtected: Boolean) {
        clearHandler.removeMessages(ClipboardClearHandler.MESSAGE_CLEAR)
        clipboard.setPrimaryClip(createClipData(text, isProtected))
        isDirty.set(true)
    }

    fun copyWithTimeout(text: String, isProtected: Boolean, timeout: Duration) {
        copyWithTimeout(
            text = text,
            isProtected = isProtected,
            timeoutInMillis = timeout.toMillis()
        )
    }

    fun copyWithTimeout(text: String, isProtected: Boolean, timeoutInMillis: Long) {
        clearHandler.removeMessages(ClipboardClearHandler.MESSAGE_CLEAR)
        clipboard.setPrimaryClip(createClipData(text, isProtected))
        isDirty.set(true)

        clearHandler.sendMessageDelayed(
            clearHandler.obtainMessage(ClipboardClearHandler.MESSAGE_CLEAR),
            timeoutInMillis
        )
    }

    fun clear() {
        clipboard.setPrimaryClip(ClipData.newPlainText(EMPTY, EMPTY))
        isDirty.set(false)
    }

    fun clearIfNeed() {
        if (isDirty.get()) {
            isDirty.set(false)
            clipboard.setPrimaryClip(ClipData.newPlainText(EMPTY, EMPTY))
        }
    }

    private fun createClipData(text: String, isProtected: Boolean): ClipData {
        val sensitive = PersistableBundle()
            .apply {
                if (isProtected) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    } else {
                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                    }
                }
            }

        return ClipData.newPlainText(EMPTY, text)
            .apply {
                description.extras = sensitive
            }
    }

    private class ClipboardClearHandler(
        private val clipboardInteractor: ClipboardInteractor
    ) : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_CLEAR) {
                clipboardInteractor.clear()
            }
        }

        companion object {
            const val MESSAGE_CLEAR = 1
        }
    }
}