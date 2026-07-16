package com.ivanovsky.passnotes.keepassrs

internal object KeepassRsAndroid {

    private const val LIBRARY_NAME = "keepass_rs"

    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    external fun nativeDecode(databaseBytes: ByteArray, keyProto: ByteArray): ByteArray?
    external fun nativeEncode(databaseProto: ByteArray, keyProto: ByteArray): ByteArray?
}
