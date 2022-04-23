package com.ivanovsky.passnotes.data.entity

enum class KeyType {
    PASSWORD,
    KEY_FILE;

    companion object {
        fun getByName(name: String): KeyType? {
            return values().firstOrNull { it.name == name }
        }
    }
}