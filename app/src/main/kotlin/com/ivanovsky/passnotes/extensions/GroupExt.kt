package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.Group

fun Group.matches(query: String): Boolean {
    return title.contains(query, ignoreCase = true)
}