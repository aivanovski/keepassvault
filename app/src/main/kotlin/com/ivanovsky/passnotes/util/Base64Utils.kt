package com.ivanovsky.passnotes.util

import android.util.Base64

object Base64Utils {

    fun toBase64String(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    fun fromBase64String(base64Data: String): ByteArray {
        return Base64.decode(base64Data, Base64.NO_WRAP)
    }
}