package com.ivanovsky.passnotes.utils

import com.ivanovsky.passnotes.data.crypto.DataCipher

class NullDataCipher : DataCipher {
    override fun encode(data: String): String? = null
    override fun decode(data: String): String? = null
}