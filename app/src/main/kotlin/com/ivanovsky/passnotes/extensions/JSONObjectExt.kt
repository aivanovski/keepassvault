package com.ivanovsky.passnotes.extensions

import org.json.JSONObject

fun JSONObject.optStringArray(name: String): Array<String> {
    if (!has(name)) {
        return emptyArray()
    }

    val result = mutableListOf<String>()
    val arrayObj = getJSONArray(name)
    for (idx in 0 until arrayObj.length()) {
        result.add(arrayObj.getString(idx))
    }

    return result.toTypedArray()
}