package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.getEntry
import app.keemobile.kotpass.database.modifiers.binaries
import app.keemobile.kotpass.database.modifiers.modifyBinaries
import app.keemobile.kotpass.database.modifiers.modifyGroup
import app.keemobile.kotpass.database.modifiers.removeEntry
import app.keemobile.kotpass.models.BinaryData
import app.keemobile.kotpass.models.BinaryReference
import app.keemobile.kotpass.models.Entry
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_NOT_FOUND
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_NOTE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.NoteDiffer.DiffAction
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.getOrNull
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.extensions.matches
import com.ivanovsky.passnotes.extensions.toByteString
import java.util.UUID
import kotlin.concurrent.withLock
import kotlin.math.max
import okio.ByteString

class KotpassNoteDao(
    private val db: KotpassDatabase
) : NoteDao {

    private val watcher = ContentWatcher<Note>()
    private val differ = NoteDiffer()

    override fun getContentWatcher(): ContentWatcher<Note> = watcher

    override fun getAll(): OperationResult<List<Note>> {
        return db.lock.withLock {
            val root = db.getRawRootGroup()

            val allNotes = db.collectEntries(root) { rawGroup, rawGroupEntries ->
                rawGroupEntries.convertToNotes(
                    groupUid = rawGroup.uuid,
                    allBinaries = db.getRawDatabase().binaries
                )
            }

            OperationResult.success(allNotes)
        }
    }

    override fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>> {
        return db.lock.withLock {
            val getGroupResult = db.getRawGroupByUid(groupUid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val rawGroup = getGroupResult.obj
            return@withLock OperationResult.success(
                rawGroup.entries.convertToNotes(
                    groupUid = rawGroup.uuid,
                    allBinaries = db.getRawDatabase().binaries
                )
            )
        }
    }

    override fun getNoteByUid(noteUid: UUID): OperationResult<Note> {
        return db.lock.withLock {
            val result = db.getRawDatabase().getEntry { rawEntry -> rawEntry.uuid == noteUid }
                ?: return@withLock OperationResult.error(
                    newDbError(
                        String.format(
                            GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID,
                            Note::class.simpleName,
                            noteUid
                        ),
                        Stacktrace()
                    )
                )

            val (rawGroup, rawEntry) = result

            OperationResult.success(
                rawEntry.convertToNote(
                    groupUid = rawGroup.uuid,
                    allBinaries = db.getRawDatabase().binaries
                )
            )
        }
    }

    override fun insert(note: Note): OperationResult<UUID> {
        return insert(
            note,
            notifyWatcher = true,
            doCommit = true
        )
    }

    override fun insert(notes: List<Note>): OperationResult<Boolean> {
        return insert(
            notes,
            doCommit = true
        )
    }

    override fun insert(notes: List<Note>, doCommit: Boolean): OperationResult<Boolean> {
        val results = notes.map { note ->
            insert(
                note,
                notifyWatcher = false,
                doCommit = doCommit
            )
        }

        val isSuccess = results.all { it.isSucceededOrDeferred }

        return if (isSuccess) {
            val commitResult = if (doCommit) {
                val commit = db.commit()
                if (commit.isFailed) {
                    return commit.mapError()
                }
                commit
            } else {
                null
            }

            val newNotes = notes.mapIndexed { idx, note ->
                val uid = results[idx].obj
                note.copy(uid = uid)
            }

            watcher.notifyEntriesInserted(newNotes)

            commitResult ?: OperationResult.success(true)
        } else {
            val failedOperation = results.firstOrNull { it.isFailed }
            if (failedOperation != null) {
                failedOperation.mapError()
            } else {
                OperationResult.error(
                    newDbError(
                        String.format(GENERIC_MESSAGE_NOT_FOUND, "Operation"),
                        Stacktrace()
                    )
                )
            }
        }
    }

    override fun update(newNote: Note, doCommit: Boolean): OperationResult<UUID> {
        val noteUid = newNote.uid
            ?: return OperationResult.error(
                newDbError(
                    MESSAGE_UID_IS_NULL,
                    Stacktrace()
                )
            )

        val getOldNoteResult = db.lock.withLock { getNoteByUid(noteUid) }
        if (getOldNoteResult.isFailed) {
            return getOldNoteResult.takeError()
        }
        val oldNote = getOldNoteResult.obj

        val result = db.lock.withLock {
            val getOldEntryAndGroupResult = db.getRawEntryAndGroupByUid(noteUid)
            if (getOldEntryAndGroupResult.isFailed) {
                return@withLock getOldEntryAndGroupResult.takeError()
            }

            val (oldRawGroup, oldRawEntry) = getOldEntryAndGroupResult.getOrThrow()

            val isInTheSameGroup = (newNote.groupUid == oldNote.groupUid)

            val prepareHistoryResult = prepareEntryHistory(oldRawEntry)
            if (prepareHistoryResult.isFailed) {
                return@withLock prepareHistoryResult.mapError()
            }

            val newHistory = prepareHistoryResult.getOrThrow()
            val newEntry = newNote.convertToEntry(history = newHistory)

            val oldEntryIdx = oldRawGroup.entries.indexOfFirst { it.uuid == noteUid }
            if (oldEntryIdx == -1) {
                return@withLock OperationResult.error(
                    newDbError(
                        MESSAGE_FAILED_TO_FIND_NOTE,
                        Stacktrace()
                    )
                )
            }

            var newDb = db.getRawDatabase()

            val (toInsert, toRemove) = prepareAttachmentsDiff(
                oldEntry = oldRawEntry,
                oldBinariesMap = db.getRawDatabase().binaries,
                newNote = newNote,
                newHistory = newHistory
            )

            if (toInsert.isNotEmpty() || toRemove.isNotEmpty()) {
                newDb = modifyBinaries(
                    noteUid = oldNote.uid,
                    toInsert = toInsert,
                    toRemove = toRemove
                )
            }

            if (isInTheSameGroup) {
                newDb = newDb.modifyGroup(newNote.groupUid) {
                    copy(
                        entries = entries.toMutableList()
                            .apply {
                                this[oldEntryIdx] = newEntry
                            }
                    )
                }
            } else {
                val getNewGroupResult = db.getRawGroupByUid(newNote.groupUid)
                if (getNewGroupResult.isFailed) {
                    return@withLock getNewGroupResult.mapError()
                }

                newDb = newDb.modifyGroup(oldNote.groupUid) {
                    copy(
                        entries = entries.toMutableList()
                            .apply {
                                removeAt(oldEntryIdx)
                            }
                    )
                }

                newDb = newDb.modifyGroup(newNote.groupUid) {
                    copy(
                        entries = entries.toMutableList()
                            .apply {
                                add(newEntry)
                            }
                    )
                }
            }

            db.swapDatabase(newDb)

            db.commit().mapWithObject(noteUid)
        }

        if (result.isSucceededOrDeferred) {
            watcher.notifyEntryChanged(oldNote, newNote)
        }

        return result
    }

    private fun prepareEntryHistory(oldEntry: Entry): OperationResult<List<Entry>> {
        val getConfigResult = db.config
        if (getConfigResult.isFailed) {
            return getConfigResult.mapError()
        }

        val config = getConfigResult.getOrThrow()
        val history = if (config.maxHistoryItems > 0) {
            val excessiveHistoryItems = max(
                0,
                oldEntry.history.size + 1 - config.maxHistoryItems
            )

            oldEntry.history
                .drop(excessiveHistoryItems)
                .toMutableList()
                .apply {
                    add(oldEntry.copy(history = emptyList()))
                }
        } else {
            emptyList()
        }

        return OperationResult.success(history)
    }

    private fun List<BinaryReference>.toAttachments(
        allBinaries: Map<ByteString, BinaryData>
    ): List<Attachment> {
        val attachments = mutableMapOf<ByteString, Attachment>()

        for (binary in this) {
            val key = binary.hash
            if (attachments.containsKey(key)) {
                continue
            }

            val attachment = binary.toAttachment(allBinaries)
            if (attachment != null) {
                attachments[key] = attachment
            }
        }

        return attachments.values.toList()
    }

    private fun prepareAttachmentsDiff(
        oldEntry: Entry,
        oldBinariesMap: Map<ByteString, BinaryData>,
        newNote: Note,
        newHistory: List<Entry>
    ): Pair<List<Attachment>, List<Attachment>> {
        val oldAttachments = mutableListOf<BinaryReference>()
            .apply {
                addAll(oldEntry.binaries)

                for (historyEntry in oldEntry.history) {
                    addAll(historyEntry.binaries)
                }
            }
            .distinctBy { reference -> reference.hash }
            .toAttachments(
                allBinaries = oldBinariesMap
            )

        val newHistoryAttachments = newHistory
            .flatMap { entry -> entry.binaries }
            .toAttachments(allBinaries = oldBinariesMap)

        val newAttachments = (newHistoryAttachments + newNote.attachments)
            .distinctBy { attachment -> attachment.hash }

        val attachmentsDiff = differ.getAttachmentsDiff(oldAttachments, newAttachments)
        return if (attachmentsDiff.isNotEmpty()) {
            val toInsert = attachmentsDiff
                .mapNotNull { (action, attachment) ->
                    if (action == DiffAction.INSERT) {
                        attachment
                    } else {
                        null
                    }
                }

            val toRemove = attachmentsDiff
                .mapNotNull { (action, attachment) ->
                    if (action == DiffAction.REMOVE) {
                        attachment
                    } else {
                        null
                    }
                }

            toInsert to toRemove
        } else {
            emptyList<Attachment>() to emptyList()
        }
    }

    override fun remove(noteUid: UUID): OperationResult<Boolean> {
        val result = db.lock.withLock {
            val getNoteResult = getNoteByUid(noteUid)
            if (getNoteResult.isFailed) {
                return@withLock getNoteResult.mapError()
            }

            val getRecycleBinResult = db.getRecycleBinGroup()
            if (getRecycleBinResult.isFailed) {
                return getRecycleBinResult.mapError()
            }

            val note = getNoteResult.obj
            val recycleBinGroup = getRecycleBinResult.getOrNull()

            val isInsideRecycleBin = if (recycleBinGroup != null) {
                val isInsideRecycleBinResult = db.isEntryInsideGroupTree(
                    entryUid = noteUid,
                    groupTreeRootUid = recycleBinGroup.uuid
                )

                if (isInsideRecycleBinResult.isFailed) {
                    return@withLock isInsideRecycleBinResult.mapError()
                }

                isInsideRecycleBinResult.getOrThrow()
            } else {
                false
            }

            when {
                recycleBinGroup != null && !isInsideRecycleBin -> {
                    // move to recycle bin
                    val newNote = note.copy(groupUid = recycleBinGroup.uuid)
                    val updateResult = update(newNote, doCommit = false)
                    if (updateResult.isFailed) {
                        return@withLock updateResult.mapError()
                    }
                }

                else -> {
                    // remove permanently
                    val newDb = db.getRawDatabase().removeEntry(noteUid)
                    db.swapDatabase(newDb)
                }
            }

            db.commit().mapWithObject(note)
        }

        if (result.isSucceededOrDeferred) {
            val note = result.obj
            watcher.notifyEntryRemoved(note)
        }

        return result.mapWithObject(true)
    }

    override fun find(query: String): OperationResult<List<Note>> {
        return db.lock.withLock {
            val allNotesResult = getAll()
            if (allNotesResult.isFailed) {
                return@withLock allNotesResult.mapError()
            }

            val matchedNotes = allNotesResult.obj
                .filter { it.matches(query) }

            OperationResult.success(matchedNotes)
        }
    }

    private fun insert(
        note: Note,
        notifyWatcher: Boolean,
        doCommit: Boolean
    ): OperationResult<UUID> {
        val newUid = UUID.randomUUID()
        val newNote = note.copy(uid = newUid)

        val result = db.lock.withLock {
            val getGroupResult = db.getRawGroupByUid(newNote.groupUid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val rawGroup = getGroupResult.obj
            val rawEntry = newNote.convertToEntry()

            val newEntries = rawGroup.entries.plus(rawEntry)

            var newDb = db.getRawDatabase()

            if (note.attachments.isNotEmpty()) {
                newDb = modifyBinaries(
                    noteUid = null,
                    toInsert = note.attachments,
                    toRemove = emptyList()
                )
            }

            newDb = newDb.modifyGroup(newNote.groupUid) {
                copy(
                    entries = newEntries
                )
            }

            db.swapDatabase(newDb)

            if (doCommit) {
                db.commit().mapWithObject(newUid)
            } else {
                OperationResult.success(newUid)
            }
        }

        if (notifyWatcher && result.isSucceededOrDeferred) {
            watcher.notifyEntryInserted(newNote)
        }

        return result
    }

    private fun modifyBinaries(
        noteUid: UUID?,
        toInsert: List<Attachment>,
        toRemove: List<Attachment>
    ): KeePassDatabase {
        val root = db.getRawRootGroup()
        val allEntries = db.collectEntries(root) { _, entries ->
            entries.filter { entry -> entry.uuid != noteUid }
        }

        val removeSet = toRemove
            .map { attachment -> attachment.hash.toByteString() }
            .toSet()

        val skip = mutableSetOf<Hash>()

        for (entry in allEntries) {
            for (binary in entry.binaries) {
                if (binary.hash in removeSet) {
                    skip.add(Hash(binary.hash.toByteArray(), HashType.SHA_256))
                }
            }
        }

        return db.getRawDatabase().modifyBinaries {
            val binaryMap = it.toMutableMap()

            for (attachment in toInsert) {
                val key = attachment.hash.toByteString()
                if (!binaryMap.containsKey(key)) {
                    binaryMap[key] = attachment.convertToBinaryData()
                }
            }

            for (attachment in toRemove) {
                if (attachment.hash in skip) {
                    continue
                }

                val key = attachment.hash.toByteString()
                binaryMap.remove(key)
            }

            binaryMap
        }
    }

    override fun getHistory(uid: UUID): OperationResult<List<Note>> {
        return db.lock.withLock {
            val getEntryAndGroupResult = db.getRawEntryAndGroupByUid(uid)
            if (getEntryAndGroupResult.isFailed) {
                return@withLock getEntryAndGroupResult.mapError()
            }

            val (group, entry) = getEntryAndGroupResult.getOrThrow()

            val history = entry.history
                .map { historyEntry ->
                    historyEntry.convertToNote(
                        groupUid = group.uuid,
                        allBinaries = db.getRawDatabase().binaries
                    )
                }

            OperationResult.success(history)
        }
    }
}