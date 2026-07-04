package com.ivanovsky.passnotes.keepassrs

open class KeepassRsException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

class InvalidProtobufException(
    cause: com.google.protobuf.InvalidProtocolBufferException
) : KeepassRsException(cause = cause)

class InvalidKeyException(
    message: String
) : KeepassRsException(message = message)