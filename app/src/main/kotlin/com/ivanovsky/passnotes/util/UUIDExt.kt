package com.ivanovsky.passnotes.util

import java.util.UUID

fun UUID.toCleanString(): String {
    return this.toString().replace("-", "")
}