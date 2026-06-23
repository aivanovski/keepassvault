package com.ivanovsky.passnotes.domain.rust

import com.google.protobuf.ByteString
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Compression
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Database as ProtoDatabase
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.DatabaseFormat
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.InnerCipher
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.OuterCipher
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.ReadDatabaseResponse
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Times
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.WriteDatabaseRequest
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.argon2Kdf
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.database
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.databaseConfig
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.databaseMeta
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.databaseOrNull
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.databaseVersion
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.entry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.field
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.group
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.kdfConfig
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.readDatabaseResponse
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.times
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.writeDatabaseRequest
import java.nio.ByteBuffer
import java.util.UUID

@Suppress("unused")
object RustBridgeProtobufExample {

    fun serializeReadResponse(database: ProtoDatabase): ByteArray {
        return readDatabaseResponse {
            this.database = database
        }.toByteArray()
    }

    fun deserializeReadResponse(bytes: ByteArray): ProtoDatabase {
        return ReadDatabaseResponse.parseFrom(bytes).databaseOrNull
            ?: error("ReadDatabaseResponse does not contain database")
    }

    fun serializeWriteRequest(database: ProtoDatabase): ByteArray {
        return writeDatabaseRequest {
            this.database = database
        }.toByteArray()
    }

    fun deserializeWriteRequest(bytes: ByteArray): ProtoDatabase {
        return WriteDatabaseRequest.parseFrom(bytes).databaseOrNull
            ?: error("WriteDatabaseRequest does not contain database")
    }

    fun createExampleDatabase(): ProtoDatabase {
        val rootGroupUid = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val entryUid = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val now = System.currentTimeMillis()

        val entry = entry {
            uuid = entryUid.toByteString()
            parentGroupUuid = rootGroupUid.toByteString()
            times = createExampleTimes(now)
            fields += field {
                name = "Title"
                value = "Example entry"
                isProtected = false
            }
            fields += field {
                name = "Password"
                value = "secret"
                isProtected = true
            }
            qualityCheck = true
        }

        val root = group {
            uuid = rootGroupUid.toByteString()
            name = "Database"
            times = createExampleTimes(now)
            isExpanded = true
            entries += entry
        }

        return database {
            config = createExampleConfig()
            meta = databaseMeta {
                generator = "KeePassVault"
                databaseName = "Example"
                recycleBinEnabled = false
            }
            rootGroup = root
        }
    }

    private fun createExampleConfig() = databaseConfig {
        version = databaseVersion {
            format = DatabaseFormat.DATABASE_FORMAT_KDBX4
            major = 4
            minor = 1
        }
        outerCipher = OuterCipher.OUTER_CIPHER_AES_256
        compression = Compression.COMPRESSION_GZIP
        innerCipher = InnerCipher.INNER_CIPHER_CHACHA20
        kdf = kdfConfig {
            argon2Id = argon2Kdf {
                iterations = 50
                memoryKib = 1024 * 1024
                parallelism = 4
                version = 0x13
            }
        }
    }

    private fun createExampleTimes(timestamp: Long): Times {
        return times {
            creationEpochMs = timestamp
            lastModificationEpochMs = timestamp
            expires = false
        }
    }

    private fun UUID.toByteString(): ByteString {
        val buffer = ByteBuffer.allocate(UUID_SIZE_BYTES)
            .putLong(mostSignificantBits)
            .putLong(leastSignificantBits)

        return ByteString.copyFrom(buffer.array())
    }

    private const val UUID_SIZE_BYTES = 16
}
