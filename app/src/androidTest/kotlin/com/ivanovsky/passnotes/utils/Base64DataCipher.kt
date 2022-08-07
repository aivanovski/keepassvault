package com.ivanovsky.passnotes.utils

import android.util.Base64
import com.ivanovsky.passnotes.data.crypto.DataCipher

class Base64DataCipher : DataCipher {

    override fun encode(data: String): String? {
        return Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP)
    }

    override fun decode(data: String): String {
        return String(Base64.decode(data, Base64.NO_WRAP))
    }
}