package com.ivanovsky.passnotes.domain.rust

object RustBridge {

    private const val LIBRARY_NAME = "passnotes_rust"

    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    external fun nativeAdd(left: Int, right: Int): Int
}
