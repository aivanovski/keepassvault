package com.ivanovsky.passnotes.data.repository.file

enum class FSType(val value: String) {
    REGULAR_FS("REGULAR_FS"),
    DROPBOX("DROPBOX");

    companion object {

        @JvmStatic
        fun findByValue(value: String): FSType? {
            return values().firstOrNull { it.value == value }
        }
    }
}