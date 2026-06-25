package com.ivanovsky.passnotes.data.repository.keepass

import com.github.aivanovski.keepasstreebuilder.DatabaseBuilderDsl
import com.github.aivanovski.keepasstreebuilder.Fields
import com.github.aivanovski.keepasstreebuilder.generator.EntityFactory.newBinaryFrom
import com.github.aivanovski.keepasstreebuilder.model.Binary
import com.github.aivanovski.keepasstreebuilder.model.DatabaseKey
import com.github.aivanovski.keepasstreebuilder.model.EntryEntity
import com.github.aivanovski.keepasstreebuilder.model.GroupEntity
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.repository.keepass.model.DatabaseFixture
import com.ivanovsky.passnotes.data.repository.keepass.model.DatabaseSize
import com.ivanovsky.passnotes.data.repository.keepass.model.HashAlgorithm
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

object DatabaseContentFactory {

    const val PASSWORD = "13cB1D91Nqr5gxHCg2Vh"

    private val FIXED_TIME = Instant.parse("2024-01-01T00:00:00Z")
    private val ROOT_GROUP = GroupEntity(
        uuid = UUID.nameUUIDFromBytes("root".toByteArray()),
        fields = mapOf(
            Fields.TITLE to "Benchmark"
        )
    )

    fun createDatabase(
        size: DatabaseSize,
        hash: HashAlgorithm
    ): DatabaseFixture {
        val bytes = buildDatabaseBytes(size.sizeInBytes, hash)
        return DatabaseFixture(
            hash = hash,
            requestedSize = size,
            bytes = bytes
        )
    }

    fun createPayloadBytes(sizeBytes: Int): ByteArray {
        val bytes = ByteArray(sizeBytes)

        for (index in bytes.indices) {
            bytes[index] = Random.nextInt(0, 256).toByte()
        }

        return bytes
    }

    private fun buildDatabaseBytes(
        requestedPayloadSize: Long,
        hash: HashAlgorithm
    ): ByteArray {
        var payloadSize = 0L

        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter(hash))
            .key(DatabaseKey.PasswordKey(PASSWORD))
            .content(ROOT_GROUP) {
                group(newGroup("Email")) {
                    entry(newEntry(index = 1, title = "Proton Mail"))
                    entry(newEntry(index = 2, title = "Outlook"))
                }
                group(newGroup("Internet")) {
                    entry(newEntry(index = 3, title = "Google"))
                    entry(newEntry(index = 4, title = "Github"))
                    entry(newEntry(index = 5, title = "Dropbox"))
                }

                var payloadIndex = 1
                while (payloadSize < requestedPayloadSize) {
                    val payload = createPayloadBytes(2024)

                    entry(
                        newEntry(
                            index = payloadIndex + 100,
                            title = "Payload $payloadIndex",
                            notes = "Synthetic payload for benchmark",
                            attachments = listOf(
                                newBinaryFrom(
                                    name = "payload-$payloadIndex.bin",
                                    content = payload
                                )
                            )
                        )
                    )

                    payloadIndex++
                    payloadSize += payload.size
                }
            }
            .build()
            .contentFactory
            .invoke()
            .use { input -> input.readBytes() }
    }

    private fun newGroup(title: String): GroupEntity {
        return GroupEntity(
            uuid = UUID.nameUUIDFromBytes("group:$title".toByteArray()),
            fields = mapOf(
                Fields.TITLE to title
            )
        )
    }

    private fun newEntry(
        index: Int,
        title: String,
        notes: String = "Benchmark entry",
        attachments: List<Binary> = emptyList()
    ): EntryEntity {
        return EntryEntity(
            uuid = UUID.nameUUIDFromBytes("entry:$index:$title".toByteArray()),
            created = FIXED_TIME,
            modified = FIXED_TIME,
            expires = null,
            fields = mapOf(
                PropertyType.TITLE.propertyName to title,
                PropertyType.USER_NAME.propertyName to "john.doe.$index@example.com",
                PropertyType.PASSWORD.propertyName to "abc123",
                PropertyType.URL.propertyName to "https://example.com/$index",
                PropertyType.NOTES.propertyName to notes
            ),
            history = emptyList(),
            binaries = attachments
        )
    }
}