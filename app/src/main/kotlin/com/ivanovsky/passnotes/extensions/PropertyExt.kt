package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.Property

fun Property.matches(query: String): Boolean {
    val isNameMatches = name?.contains(query, ignoreCase = true)
    val isValueMatches = value?.contains(query, ignoreCase = true)
    return isNameMatches == true || isValueMatches == true
}