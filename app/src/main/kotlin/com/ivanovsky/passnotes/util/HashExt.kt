package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.data.entity.Hash

fun Hash.format(): String {
    val base64 = Base64Utils.toBase64String(data)
    return "$type:$base64"
}