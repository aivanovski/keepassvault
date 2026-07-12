package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.matches
import com.ivanovsky.passnotes.keepassrs.proto.v1.Attachment as ProtoAttachment
import com.ivanovsky.passnotes.keepassrs.proto.v1.Entry as ProtoEntry
import com.ivanovsky.passnotes.keepassrs.proto.v1.EntryAttachment
import com.ivanovsky.passnotes.keepassrs.proto.v1.Group as ProtoGroup
import com.ivanovsky.passnotes.keepassrs.proto.v1.timesOrNull
import com.ivanovsky.passnotes.util.ShaUtils
import com.ivanovsky.passnotes.util.format
import com.ivanovsky.passnotes.util.toOperationResult
import java.util.LinkedList
import java.util.UUID
import kotlin.concurrent.withLock
import kotlin.math.max
import timber.log.Timber

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
                        entry.convertToNote(
                            groupUid = groupUid,
                            allAttachments = db.getRawDatabase().getAllAttachmentsMap()
                        )
                    }
            )
        }
    }

    override fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> {
        return db.lock.withLock {
            either {
                val group = db.getRawGroupByUid(groupUid).bind()
                val attachmentsMap = db.getRawDatabase().getAllAttachmentsMap()

                group.entriesList.map { entry ->
                    entry.convertToNote(
                        groupUid = groupUid,
                        allAttachments = attachmentsMap
                    )
                }
            }.toOperationResult()
        }
    }

    override fun getNoteByUid(noteUid: UUID): OperationResult<Note> =
        db.lock.withLock {
            either {
                val (group, entry) = db.getRawEntryWithGroupByUid(noteUid).bind()

                val groupUid = group.uuid.toUuid().bind()

                entry.convertToNote(
                    groupUid = groupUid,
                    allAttachments = db.getRawDatabase().getAllAttachmentsMap()
                )
            }.toOperationResult()
        }

    override fun insert(note: Note): OperationResult<UUID> =
        insert(note, doCommit = true)

    private fun insert(n: Note, doCommit: Boolean): OperationResult<UUID> {
        val uid = n.uid ?: UUID.randomUUID()
        val note = n.copy(uid = uid)

        val result = db.lock.withLock {
            either {
                db.getRawGroupByUid(note.groupUid).bind()

                val (newAttachments, attachmentHashToIdMap) = prepareAttachmentList(
                    updatedNoteUid = null,
                    toInsert = note.attachments,
                    toRemove = emptyList()
                )

                val protoEntry = note.toProtoEntry(
                    attachmentHashToIdMap = attachmentHashToIdMap
                )

                db.swapDatabase { db ->
                    db.toBuilder()
                        .clearAttachments()
                        .addAllAttachments(newAttachments)
                        .setRootGroup(
                            db.rootGroup.updateGroup(note.groupUid) { protoGroup ->
                                protoGroup.toBuilder()
                                    .addEntries(protoEntry)
                                    .build()
                            }
                        )
                        .build()
                }

                if (doCommit) {
                    db.commit()
                        .map { uid }
                        .bind()
                } else {
                    uid
                }
            }.toOperationResult()
        }

        if (doCommit && result.isSucceededOrDeferred) {
            watcher.notifyEntryInserted(note)
        }

        return result
    }

    override fun insert(notes: List<Note>): OperationResult<Boolean> =
        insert(notes, doCommit = true)

    override fun insert(notes: List<Note>, doCommit: Boolean): OperationResult<Boolean> {
        val inserted = mutableListOf<Note>()
        for (note in notes) {
            val result = insert(n = note, doCommit = false)
            if (result.isFailed) {
                return result.mapError()
            }
            inserted.add(note.copy(uid = result.obj))
        }

        val commitResult =
            if (doCommit) db.commit().toOperationResult() else OperationResult.success(true)
        if (commitResult.isSucceededOrDeferred) {
            watcher.notifyEntriesInserted(inserted)
        }

        return commitResult
    }

    override fun update(newNote: Note, doCommit: Boolean): OperationResult<UUID> {
        val noteUid = newNote.uid ?: return OperationResult.error(
            newDbError(MESSAGE_UID_IS_NULL, Stacktrace())
        )

        val oldNoteResult = getNoteByUid(noteUid)
        if (oldNoteResult.isFailed) {
            return oldNoteResult.mapError()
        }

        val result = db.lock.withLock {
            either {
                val (_, oldRawEntry) = db.getRawEntryWithGroupByUid(noteUid).bind()
                val oldAttachmentsMap = db.getRawDatabase().getAllAttachmentsMap()
                val newHistory = prepareEntryHistory(oldRawEntry).bind()

                val (toInsert, toRemove) = prepareAttachmentsDiff(
                    oldEntry = oldRawEntry,
                    oldAttachmentsMap = oldAttachmentsMap,
                    newNote = newNote,
                    newHistory = newHistory
                )

                val (newAttachments, attachmentHashToIdMap) = prepareAttachmentList(
                    updatedNoteUid = noteUid,
                    toInsert = toInsert,
                    toRemove = toRemove
                )

                val newEntry = newNote.toProtoEntry(
                    attachmentHashToIdMap = attachmentHashToIdMap,
                    history = newHistory
                )

                db.swapDatabase { db ->
                    db.toBuilder()
                        .clearAttachments()
                        .addAllAttachments(newAttachments)
                        .setRootGroup(
                            db.rootGroup
                                .removeEntry(noteUid)
                                .updateGroup(newNote.groupUid) { group ->
                                    group.toBuilder()
                                        .addEntries(newEntry)
                                        .build()
                                }
                        )
                        .build()
                }

                if (doCommit) {
                    db.commit()
                        .map { noteUid }
                        .bind()
                } else {
                    noteUid
                }
            }
        }

        if (result.isRight()) {
            watcher.notifyEntryChanged(oldNoteResult.obj, newNote)
        }

        return result.toOperationResult()
    }

    override fun remove(noteUid: UUID): OperationResult<Boolean> {
        val noteResult = getNoteByUid(noteUid)
        if (noteResult.isFailed) {
            return noteResult.mapError()
        }

        db.lock.withLock {
            db.swapDatabase { db ->
                db.toBuilder()
                    .setRootGroup(db.rootGroup.removeEntry(noteUid))
                    .build()
            }
        }

        val result = db.commit().toOperationResult()
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

    override fun getHistory(noteUid: UUID): OperationResult<List<Note>> =
        db.lock.withLock {
            either {
                val (group, entry) = db.getRawEntryWithGroupByUid(noteUid).bind()
                val groupUid = group.uuid.toUuid().bind()

                entry.historyList.convertToNotes(
                    groupUid = groupUid,
                    allAttachments = db.getRawDatabase().getAllAttachmentsMap()
                ).sortedBy { note -> note.modified }
            }.toOperationResult()
        }

    private fun ProtoGroup.flattenEntries(): List<Pair<UUID, ProtoEntry>> {
        val startGroup = this

        val result = mutableListOf<Pair<UUID, ProtoEntry>>()
        val queue = LinkedList<ProtoGroup>()
            .apply {
                add(startGroup)
            }

        while (queue.isNotEmpty()) {
            val group = queue.removeFirst()
            val groupUid = group.uuid.toUuidOrThrow()

            result.addAll(group.entriesList.map { entry -> groupUid to entry })
            queue.addAll(group.groupsList)
        }

        return result
    }

    private fun prepareEntryHistory(
        oldEntry: ProtoEntry
    ): Either<OperationError, List<ProtoEntry>> =
        either {
            val config = db.getConfig().bind()

            val history = if (config.maxHistoryItems > 0) {
                val excessiveHistoryItems = max(
                    0,
                    oldEntry.historyList.size + 1 - config.maxHistoryItems
                )

                oldEntry.historyList
                    .drop(excessiveHistoryItems)
                    .sortedBy { entry -> entry.timesOrNull?.lastModificationEpochMs ?: 0L }
                    .toMutableList()
                    .apply {
                        val newEntry = oldEntry.toBuilder()
                            .clearHistory()
                            .build()

                        add(newEntry)
                    }
            } else {
                emptyList()
            }

            history
        }

    private fun prepareAttachmentsDiff(
        oldEntry: ProtoEntry,
        oldAttachmentsMap: Map<Int, ProtoAttachment>,
        newNote: Note,
        newHistory: List<ProtoEntry>
    ): Pair<List<Attachment>, List<ProtoAttachment>> {
        val oldAttachments = mutableListOf<EntryAttachment>()
            .apply {
                addAll(oldEntry.attachmentsList)

                for (historyEntry in oldEntry.historyList) {
                    addAll(historyEntry.attachmentsList)
                }
            }
            .distinctBy { attachment -> attachment.attachmentId }

        val newHistoryAttachments = newHistory
            .flatMap { entry -> entry.attachmentsList }
            .mapNotNull { attachment -> oldAttachmentsMap[attachment.attachmentId] }

        // Find attachments to insert to db
        val toInsert = mutableListOf<Attachment>()
        val usedAttachmentIds = newHistoryAttachments
            .map { attachment -> attachment.id }
            .toMutableSet()
        for (attachment in newNote.attachments) {
            val attachmentId = attachment.uid.split("#")
                .firstOrNull()
                ?.toIntOrNull()

            if (attachmentId != null) {
                if (attachmentId in oldAttachmentsMap) {
                    usedAttachmentIds.add(attachmentId)
                }
            } else {
                toInsert.add(attachment)
            }
        }

        // Find attachments to remove
        val toRemove = mutableListOf<ProtoAttachment>()
        for (entryAttachment in oldAttachments) {
            val attachment = oldAttachmentsMap[entryAttachment.attachmentId] ?: continue

            if (attachment.id !in usedAttachmentIds) {
                toRemove.add(attachment)
            }
        }

        if (BuildConfig.DEBUG) {
            Timber.d("Adding ${toInsert.size} attachments:")
            for (attachment in toInsert) {
                val uid = attachment.uid
                val name = attachment.name
                val hash = attachment.hash.format()
                Timber.d("    + uid=$uid, $name, hash=$hash")
            }

            Timber.d("Removing ${toRemove.size} attachments:")
            for (attachment in toRemove) {
                val hash = ShaUtils.sha256(attachment.data.toByteArray()).format()
                Timber.d("    - id=${attachment.id}, hash=$hash")
            }
        }

        return toInsert to toRemove
    }

    private fun prepareAttachmentList(
        updatedNoteUid: UUID?,
        toInsert: List<Attachment>,
        toRemove: List<ProtoAttachment>
    ): Pair<List<ProtoAttachment>, Map<Hash, Int>> {
        val attachmentHashToIdMap = db.getRawDatabase()
            .attachmentsList
            .associate { attachment -> attachment.toHash() to attachment.id }
            .toMutableMap()

        if (toInsert.isEmpty() && toRemove.isEmpty()) {
            return db.getRawDatabase().attachmentsList to attachmentHashToIdMap
        }

        val otherEntries = db.getRawDatabase()
            .rootGroup
            .collectEntries { _, entries ->
                entries.filter { entry ->
                    val entryUid = entry.uuid.toUuidOrNull()
                    entryUid != null && entryUid != updatedNoteUid
                }
            }

        val removeIdSet = toRemove
            .map { attachment -> attachment.id }
            .toMutableSet()

        val attachmentIds = db.getRawDatabase().getAllAttachmentsMap().keys

        val idPoolSet = attachmentIds
            .filter { id -> id in removeIdSet }
            .toMutableSet()

        for (entry in otherEntries) {
            for (attachment in entry.getAllAttachments()) {
                // Removed attachment is used by other entry
                val id = attachment.attachmentId
                if (id in removeIdSet) {
                    removeIdSet.remove(id)
                    idPoolSet.remove(id)
                }
            }
        }

        val newAttachmentList = mutableListOf<ProtoAttachment>()
        val oldAttachmentList = db.getRawDatabase().attachmentsList
        for (oldAttachment in oldAttachmentList) {
            val id = oldAttachment.id

            if (id !in removeIdSet) {
                newAttachmentList.add(oldAttachment)
            }
        }

        val idPool = LinkedList<Int>()
            .apply {
                addAll(idPoolSet.sorted())
            }

        var nextId = if (attachmentIds.isNotEmpty()) {
            attachmentIds.max()
        } else {
            0
        }

        for (newAttachment in toInsert) {
            val id = if (idPool.isNotEmpty()) {
                idPool.removeFirst()
            } else {
                val next = nextId + 1
                nextId += 1
                next
            }

            newAttachmentList.add(newAttachment.toProtoAttachment(id))
            attachmentHashToIdMap[newAttachment.hash] = id
        }

        newAttachmentList.sortBy { attachment -> attachment.id }

        return newAttachmentList to attachmentHashToIdMap
    }

    private fun ProtoEntry.getAllAttachments(): List<EntryAttachment> {
        return attachmentsList + historyList.flatMap { entry -> entry.attachmentsList }
    }

    private fun ProtoAttachment.toHash(): Hash {
        return Hash(
            data = ShaUtils.sha256(data.toByteArray()).data,
            type = HashType.SHA_256
        )
    }
}