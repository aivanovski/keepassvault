package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.matches
import java.util.LinkedList
import java.util.UUID
import kotlin.concurrent.withLock
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

    override fun insert(note: Note): OperationResult<UUID> = unsupportedWriteOperation()

    override fun insert(notes: List<Note>): OperationResult<Boolean> = unsupportedWriteOperation()

    override fun insert(notes: List<Note>, doCommit: Boolean): OperationResult<Boolean> =
        unsupportedWriteOperation()

    override fun update(note: Note, doCommit: Boolean): OperationResult<UUID> =
        unsupportedWriteOperation()

    override fun remove(noteUid: UUID): OperationResult<Boolean> = unsupportedWriteOperation()

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
