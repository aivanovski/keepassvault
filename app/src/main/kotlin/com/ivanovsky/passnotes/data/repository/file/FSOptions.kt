package com.ivanovsky.passnotes.data.repository.file

data class FSOptions(
    val isCacheEnabled: Boolean,
    val isCacheOnly: Boolean,
    val isWriteEnabled: Boolean
) {

    companion object {

        val READ_ONLY = FSOptions(
            isCacheEnabled = true,
            isCacheOnly = false,
            isWriteEnabled = false
        )

        val NO_CACHE = FSOptions(
            isCacheEnabled = false,
            isCacheOnly = false,
            isWriteEnabled = true
        )

        val CACHE_ONLY = FSOptions(
            isCacheEnabled = true,
            isCacheOnly = true,
            isWriteEnabled = true
        )

        val DEFAULT = FSOptions(
            isCacheEnabled = true,
            isCacheOnly = false,
            isWriteEnabled = true
        )

        @JvmStatic
        fun noCache() = NO_CACHE

        @JvmStatic
        fun defaultOptions() = DEFAULT
    }
}