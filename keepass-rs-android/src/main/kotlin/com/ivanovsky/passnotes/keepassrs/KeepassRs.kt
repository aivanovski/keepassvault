package com.ivanovsky.passnotes.keepassrs

import arrow.core.Either
import arrow.core.raise.either
import com.google.protobuf.InvalidProtocolBufferException
import com.ivanovsky.passnotes.keepassrs.proto.v1.Database
import com.ivanovsky.passnotes.keepassrs.proto.v1.DatabaseError
import com.ivanovsky.passnotes.keepassrs.proto.v1.DatabaseErrorType
import com.ivanovsky.passnotes.keepassrs.proto.v1.DatabaseKey
import com.ivanovsky.passnotes.keepassrs.proto.v1.DecodeDatabaseResult
import com.ivanovsky.passnotes.keepassrs.proto.v1.EncodeDatabaseResult
import com.ivanovsky.passnotes.keepassrs.proto.v1.errorOrNull
import com.ivanovsky.passnotes.keepassrs.KeepassRsAndroid.nativeDecode
import com.ivanovsky.passnotes.keepassrs.KeepassRsAndroid.nativeEncode

object KeepassRs {

    fun decode(
        data: ByteArray,
        key: DatabaseKey
    ): Either<KeepassRsException, Database> =
        either {
            val protobufResult = nativeDecode(
                databaseBytes = data,
                keyProto = key.toByteArray()
            )

            val result = parseProtobuf { DecodeDatabaseResult.parseFrom(protobufResult) }.bind()

            if (result.hasDatabase()) {
                result.database
            } else {
                raise(result.errorOrNull?.toKeepassRsException() ?: KeepassRsException())
            }
        }

    private fun DatabaseError.toKeepassRsException(): KeepassRsException {
        return when (this.errorType) {
            DatabaseErrorType.INVALID_KEY -> InvalidKeyException(
                message = message.orEmpty()
            )

            else -> KeepassRsException(
                message = message.orEmpty(),
                cause = null
            )
        }
    }

    fun encode(
        database: Database,
        key: DatabaseKey
    ): Either<KeepassRsException, ByteArray> =
        either {
            val protobufResult = nativeEncode(
                databaseProto = database.toByteArray(),
                keyProto = key.toByteArray()
            )

            val result = parseProtobuf { EncodeDatabaseResult.parseFrom(protobufResult) }.bind()

            if (result.hasDatabase()) {
                result.database.toByteArray()
            } else {
                raise(
                    KeepassRsException(
                        message = result.errorOrNull?.message ?: "",
                        cause = null
                    )
                )
            }
        }
}

fun <T> parseProtobuf(converter: () -> T): Either<KeepassRsException, T> {
    return try {
        Either.Right(converter.invoke())
    } catch (exception: InvalidProtocolBufferException) {
        Either.Left(InvalidProtobufException(exception))
    }
}
