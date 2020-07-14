package com.ivanovsky.passnotes.util

import java.util.*

fun UUID.toCleanString(): String {
    return this.toString().replace("-", "")
}