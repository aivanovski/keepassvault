package com.ivanovsky.passnotes.utils

import com.ivanovsky.passnotes.data.crypto.DataCipher

class ClearTextDataCipher : DataCipher {

    override fun encode(data: String): String = data

    override fun decode(data: String): String = data
}