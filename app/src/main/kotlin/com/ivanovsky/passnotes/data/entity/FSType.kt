package com.ivanovsky.passnotes.data.entity

enum class FSType(val value: String) {
    UNDEFINED("UNDEFINED"),
    INTERNAL_STORAGE("INTERNAL_STORAGE"),
    EXTERNAL_STORAGE("EXTERNAL_STORAGE"),
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