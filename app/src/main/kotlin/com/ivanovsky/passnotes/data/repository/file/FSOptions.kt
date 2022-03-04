package com.ivanovsky.passnotes.data.repository.file

data class FSOptions(
    val isCacheEnabled: Boolean,
    val isCacheOnly: Boolean,
    val isWriteEnabled: Boolean,
    val isPostponedSyncEnabled: Boolean
) {

    companion object {

        val READ_ONLY = FSOptions(
            isCacheEnabled = true,
            isCacheOnly = false,
            isWriteEnabled = false,
            isPostponedSyncEnabled = false
        )

        val NO_CACHE = FSOptions(
            isCacheEnabled = false,
            isCacheOnly = false,
            isWriteEnabled = true,
            isPostponedSyncEnabled = false
        )

        val CACHE_ONLY = FSOptions(
            isCacheEnabled = true,
            isCacheOnly = true,
            isWriteEnabled = true,
            isPostponedSyncEnabled = false
        )

        val DEFAULT = FSOptions(
            isCacheEnabled = true,
            isCacheOnly = false,
            isWriteEnabled = true,
            isPostponedSyncEnabled = false
        )

        @JvmStatic
        fun noCache() = NO_CACHE

        @JvmStatic
        fun defaultOptions() = DEFAULT
    }
}