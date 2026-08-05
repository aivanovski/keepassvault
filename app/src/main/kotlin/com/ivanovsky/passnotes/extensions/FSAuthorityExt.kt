package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.FSAuthority

fun FSAuthority.isSyncable(): Boolean {
    return credentials != null
}