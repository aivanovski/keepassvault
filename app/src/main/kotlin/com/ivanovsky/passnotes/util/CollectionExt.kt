package com.ivanovsky.passnotes.util

fun <Key, Value> List<Pair<Key, Value>>.toLinkedMap(): LinkedHashMap<Key, Value> {
    val map = LinkedHashMap<Key, Value>()

    for (pair in this) {
        map[pair.first] = pair.second
    }

    return map
}