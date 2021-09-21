package com.ivanovsky.passnotes.data.entity

enum class FSType(val value: String) {
    REGULAR_FS("REGULAR_FS"),
    DROPBOX("DROPBOX"),
    WEBDAV("WEBDAV"),
    SAF("STORAGE_ACCESS_FRAMEWORK");

    companion object {

        @JvmStatic
        fun findByValue(value: String): FSType? {
            return values().firstOrNull { it.value == value }
        }
    }
}