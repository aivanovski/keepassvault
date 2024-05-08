package com.ivanovsky.passnotes.data.repository.file.databaseDsl

import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.modifiers.modifyGroup
import app.keemobile.kotpass.models.DatabaseElement
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.EntryFields
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Group
import app.keemobile.kotpass.models.Meta
import app.keemobile.kotpass.models.TimeData
import java.time.Instant
import java.util.UUID

object KotpassTreeDsl {

    fun newDatabase(
        credentials: Credentials,
        root: GroupEntity,
        content: (DatabaseElementBuilder.() -> Unit)? = null
    ): KeePassDatabase {
        val rootGroup = DatabaseElementBuilder(
            uuid = root.uuid,
            name = root.title
        )
            .apply {
                content?.invoke(this)
            }
            .build() as Group

        val db = KeePassDatabase.Ver4x.create(
            rootName = root.title,
            meta = Meta(recycleBinEnabled = true),
            credentials = credentials
        )

        val realRootUuid = db.content.group.uuid

        val newDb = db.modifyGroup(realRootUuid) {
            this.copy(
                uuid = rootGroup.uuid,
                groups = rootGroup.groups,
                entries = rootGroup.entries
            )
        }

        return newDb
    }

    class DatabaseElementBuilder(
        private val uuid: UUID,
        private val name: String
    ) {

        private val groups = mutableListOf<Group>()
        private val entries = mutableListOf<Entry>()

        fun group(
            entity: GroupEntity,
            content: (DatabaseElementBuilder.() -> Unit)? = null
        ) {
            val group = DatabaseElementBuilder(
                uuid = entity.uuid,
                name = entity.title
            )
                .apply {
                    content?.invoke(this)
                }
                .build() as Group

            groups.add(group)
        }

        fun entry(
            entity: EntryEntity
        ) {
            val fields = mutableMapOf(
                BasicField.Title.key to EntryValue.Plain(entity.title),
                BasicField.UserName.key to EntryValue.Plain(entity.username),
                BasicField.Password.key to EntryValue.Encrypted(
                    EncryptedValue.fromString(entity.password)
                ),
                BasicField.Url.key to EntryValue.Plain(entity.url),
                BasicField.Notes.key to EntryValue.Plain(entity.notes)
            )

            for (customField in entity.custom.entries) {
                fields[customField.key] = EntryValue.Plain(customField.value)
            }

            val expiryTime = if (entity.expires != null) {
                Instant.ofEpochMilli(entity.expires)
            } else {
                null
            }

            entries.add(
                Entry(
                    uuid = entity.uuid,
                    fields = EntryFields(fields),
                    times = TimeData(
                        creationTime = Instant.ofEpochMilli(entity.created),
                        lastModificationTime = Instant.ofEpochMilli(entity.modified),
                        lastAccessTime = null,
                        locationChanged = null,
                        expiryTime = expiryTime,
                        expires = (expiryTime != null)
                    )
                )
            )
        }

        fun build(): DatabaseElement {
            return Group(
                uuid = uuid,
                name = name,
                groups = groups,
                entries = entries
            )
        }
    }
}