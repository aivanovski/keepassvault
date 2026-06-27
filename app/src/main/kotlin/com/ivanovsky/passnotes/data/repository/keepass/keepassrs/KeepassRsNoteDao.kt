package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.extensions.matches
import java.util.LinkedList
import java.util.UUID
import kotlin.concurrent.withLock
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Database as ProtoDatabase
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Entry as ProtoEntry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Group as ProtoGroup

class KeepassRsNoteDao(
    private val db: KeepassRsDatabase
) : NoteDao {

    private val watcher = ContentWatcher<Note>()

    override fun getAll(): OperationResult<List<Note>> {
        return db.lock.withLock {
            OperationResult.success(
                db.getRawDatabase()
                    .rootGroup
                    .flattenEntries()
                    .map { (groupUid, entry) ->
                        entry.toNote(
                            groupUid = groupUid,
                            attachments = db.getRawDatabase().attachmentsList
                        )
                    }
            )
        }
    }

    override fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> {
        return db.lock.withLock {
            val group = db.getRawDatabase()
                .rootGroup
                .flattenGroups()
                .firstOrNull { group -> group.uuid.toUuidOrNull() == groupUid }
                ?: return@withLock failedToFindGroup()

            OperationResult.success(
                group.entriesList.map { entry ->
                    entry.toNote(
                        groupUid = groupUid,
                        attachments = db.getRawDatabase().attachmentsList
                    )
                }
            )
        }
    }

    override fun getNoteByUid(noteUid: UUID): OperationResult<Note> {
        return db.lock.withLock {
            val (groupUid, entry) = db.getRawDatabase()
                .rootGroup
                .flattenEntries()
                .firstOrNull { (_, entry) -> entry.uuid.toUuidOrNull() == noteUid }
                ?: return@withLock failedToFindNote(noteUid)

            OperationResult.success(
                entry.toNote(
                    groupUid = groupUid,
                    attachments = db.getRawDatabase().attachmentsList
                )
            )
        }
    }

    override fun insert(note: Note): OperationResult<UUID> = insert(note, doCommit = true)

    private fun insert(note: Note, doCommit: Boolean): OperationResult<UUID> {
        val uid = note.uid ?: UUID.randomUUID()
        val newNote = note.copy(uid = uid)

        val result = db.lock.withLock {
            db.getRawDatabase()
                .rootGroup
                .flattenGroups()
                .firstOrNull { group -> group.uuid.toUuidOrNull() == newNote.groupUid }
                ?: return@withLock failedToFindGroup()

            db.updateDatabase { database ->
                val (updatedDatabase, protoEntry) = database.addAttachmentsAndBuildEntry(newNote)
                updatedDatabase.toBuilder()
                    .setRootGroup(
                        updatedDatabase.rootGroup.updateGroup(newNote.groupUid) { protoGroup ->
                            protoGroup.toBuilder()
                                .addEntries(protoEntry)
                                .build()
                        }
                    )
                    .build()
            }

            if (doCommit) {
                db.commit().mapWithObject(uid)
            } else {
                OperationResult.success(uid)
            }
        }

        if (doCommit && result.isSucceededOrDeferred) {
            watcher.notifyEntryInserted(newNote)
        }

        return result
    }

    override fun insert(notes: List<Note>): OperationResult<Boolean> = insert(notes, doCommit = true)

    override fun insert(notes: List<Note>, doCommit: Boolean): OperationResult<Boolean> {
        val inserted = mutableListOf<Note>()
        for (note in notes) {
            val result = insert(note.copy(uid = note.uid ?: UUID.randomUUID()), doCommit = false)
            if (result.isFailed) {
                return result.mapError()
            }
            inserted.add(note.copy(uid = result.obj))
        }

        val commitResult = if (doCommit) db.commit() else OperationResult.success(true)
        if (commitResult.isSucceededOrDeferred) {
            watcher.notifyEntriesInserted(inserted)
        }

        return commitResult
    }

    override fun update(note: Note, doCommit: Boolean): OperationResult<UUID> {
        val noteUid = note.uid ?: return OperationResult.error(
            OperationError.newDbError(OperationError.MESSAGE_UID_IS_NULL, Stacktrace())
        )

        val oldNoteResult = getNoteByUid(noteUid)
        if (oldNoteResult.isFailed) {
            return oldNoteResult.mapError()
        }

        db.lock.withLock {
            db.updateDatabase { database ->
                val (updatedDatabase, protoEntry) = database.addAttachmentsAndBuildEntry(note)
                updatedDatabase.toBuilder()
                    .setRootGroup(
                        updatedDatabase.rootGroup
                            .removeEntry(noteUid)
                            .updateGroup(note.groupUid) { group ->
                                group.toBuilder()
                                    .addEntries(protoEntry)
                                    .build()
                            }
                    )
                    .build()
            }
        }

        val result = if (doCommit) db.commit().mapWithObject(noteUid) else OperationResult.success(noteUid)
        if (result.isSucceededOrDeferred) {
            watcher.notifyEntryChanged(oldNoteResult.obj, note)
        }

        return result
    }

    override fun remove(noteUid: UUID): OperationResult<Boolean> {
        val noteResult = getNoteByUid(noteUid)
        if (noteResult.isFailed) {
            return noteResult.mapError()
        }

        db.lock.withLock {
            db.updateDatabase { database ->
                database.toBuilder()
                    .setRootGroup(database.rootGroup.removeEntry(noteUid))
                    .build()
            }
        }

        val result = db.commit()
        if (result.isSucceededOrDeferred) {
            watcher.notifyEntryRemoved(noteResult.obj)
        }

        return result
    }

    override fun find(query: String): OperationResult<List<Note>> {
        return db.lock.withLock {
            val allNotesResult = all
            if (allNotesResult.isFailed) {
                return@withLock allNotesResult.takeError()
            }

            OperationResult.success(
                allNotesResult.obj.filter { note -> note.matches(query) }
            )
        }
    }

    override fun getContentWatcher(): ContentWatcher<Note> = watcher

    override fun getHistory(noteUid: UUID): OperationResult<List<Note>> {
        return db.lock.withLock {
            val (groupUid, entry) = db.getRawDatabase()
                .rootGroup
                .flattenEntries()
                .firstOrNull { (_, entry) -> entry.uuid.toUuidOrNull() == noteUid }
                ?: return@withLock failedToFindNote(noteUid)

            OperationResult.success(
                entry.historyList.map { historyEntry ->
                    historyEntry.toNote(
                        groupUid = groupUid,
                        attachments = db.getRawDatabase().attachmentsList
                    )
                }
            )
        }
    }

    private fun ProtoGroup.flattenGroups(): List<ProtoGroup> {
        val result = mutableListOf<ProtoGroup>()
        val queue = LinkedList<ProtoGroup>()
            .apply {
                add(this@flattenGroups)
            }

        while (queue.isNotEmpty()) {
            val group = queue.removeFirst()
            result.add(group)
            queue.addAll(group.groupsList)
        }

        return result
    }

    private fun ProtoGroup.flattenEntries(): List<Pair<UUID, ProtoEntry>> {
        val result = mutableListOf<Pair<UUID, ProtoEntry>>()
        val queue = LinkedList<ProtoGroup>()
            .apply {
                add(this@flattenEntries)
            }

        while (queue.isNotEmpty()) {
            val group = queue.removeFirst()
            val groupUid = group.uuid.toUuidOrThrow()

            result.addAll(group.entriesList.map { entry -> groupUid to entry })
            queue.addAll(group.groupsList)
        }

        return result
    }
}

private fun <T> failedToFindGroup(): OperationResult<T> =
    OperationResult.error(
        OperationError.newDbError(
            OperationError.MESSAGE_FAILED_TO_FIND_GROUP,
            Stacktrace()
        )
    )

private fun <T> failedToFindNote(noteUid: UUID): OperationResult<T> =
    OperationResult.error(
        OperationError.newDbError(
            String.format(
                OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                Note::class.simpleName,
                noteUid
            ),
            Stacktrace()
        )
    )

private fun ProtoDatabase.addAttachmentsAndBuildEntry(note: Note): Pair<ProtoDatabase, ProtoEntry> {
    var nextAttachmentId = (attachmentsList.maxOfOrNull { attachment -> attachment.id } ?: 0) + 1
    val newAttachments = mutableListOf<com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Attachment>()
    val entry = note.toProtoEntry { attachment ->
        val id = nextAttachmentId++
        newAttachments.add(attachment.toProtoAttachment(id))
        id
    }

    val updatedDatabase = toBuilder()
        .addAllAttachments(newAttachments)
        .build()

    return updatedDatabase to entry
}
