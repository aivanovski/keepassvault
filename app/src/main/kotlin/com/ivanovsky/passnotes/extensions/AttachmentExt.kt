package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.Hash
import okio.ByteString

fun Hash.toByteString(): ByteString = ByteString.of(*data)