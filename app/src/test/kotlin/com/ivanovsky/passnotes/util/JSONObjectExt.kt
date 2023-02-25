package com.ivanovsky.passnotes.util

import org.json.JSONArray

fun List<String>.toJSONArray(): JSONArray {
    val arrayObj = JSONArray()

    for (item in this) {
        arrayObj.put(item)
    }

    return arrayObj
}