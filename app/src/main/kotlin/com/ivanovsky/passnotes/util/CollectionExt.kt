package com.ivanovsky.passnotes.util

fun <Key, Value> List<Pair<Key, Value>>.toLinkedMap(): LinkedHashMap<Key, Value> {
    val map = LinkedHashMap<Key, Value>()

    for (pair in this) {
        map[pair.first] = pair.second
    }

    return map
}

fun <T> Collection<T>.splitAt(splitIndex: Int): Pair<List<T>, List<T>> {
    return when {
        splitIndex < 0 || splitIndex > size -> {
            throw ArrayIndexOutOfBoundsException()
        }

        splitIndex == 0 -> {
            emptyList<T>() to toList()
        }

        splitIndex == size -> {
            toList() to emptyList()
        }

        else -> {
            var idx = 0
            val left = ArrayList<T>(splitIndex)
            val right = ArrayList<T>(size - splitIndex)

            for (item in this) {
                when (idx++ >= splitIndex) {
                    false -> left.add(item)
                    true -> right.add(item)
                }
            }

            left to right
        }
    }
}