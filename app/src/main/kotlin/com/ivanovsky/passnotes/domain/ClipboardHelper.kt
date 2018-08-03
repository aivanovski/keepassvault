package com.ivanovsky.passnotes.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardHelper(context: Context) {

	private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

	fun copy(text: String) {
		clipboard.primaryClip = ClipData.newPlainText("", text)
	}

	fun clear() {
		clipboard.primaryClip = ClipData.newPlainText("", "")
	}
}