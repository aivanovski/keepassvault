package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import app.keemobile.kotpass.database.KeePassDatabase
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_NOT_FOUND
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_NOTE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.extensions.matches
import app.keemobile.kotpass.database.getEntry
import app.keemobile.kotpass.database.modifiers.binaries
import app.keemobile.kotpass.database.modifiers.modifyBinaries
import app.keemobile.kotpass.database.modifiers.modifyGroup
import app.keemobile.kotpass.database.modifiers.removeEntry
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.NoteDiffer.DiffAction
import com.ivanovsky.passnotes.extensions.toByteString
import java.util.UUID
import kotlin.concurrent.withLock

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
                        )
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

            contentWatcher.notifyEntriesInserted(newNotes)

            commitResult ?: OperationResult.success(true)
        } else {
            val failedOperation = results.firstOrNull { it.isFailed }
            if (failedOperation != null) {
                failedOperation.mapError()
            } else {
                OperationResult.error(
                    newDbError(
                        String.format(GENERIC_MESSAGE_NOT_FOUND, "Operation")
                    )
                )
            }
        }
    }

    override fun update(newNote: Note): OperationResult<UUID> {
        val noteUid = newNote.uid ?: return OperationResult.error(newDbError(MESSAGE_UID_IS_NULL))

        val getOldNoteResult = db.lock.withLock { getNoteByUid(noteUid) }
        if (getOldNoteResult.isFailed) {
            return getOldNoteResult.takeError()
        }
        val oldNote = getOldNoteResult.obj

        val result = db.lock.withLock {
            val isInTheSameGroup = (newNote.groupUid == oldNote.groupUid)

            val getOldGroupResult = db.getRawGroupByUid(oldNote.groupUid)
            if (getOldGroupResult.isFailed) {
                return@withLock getOldGroupResult.mapError()
            }

            val oldGroup = getOldGroupResult.obj
            val newEntry = newNote.convertToEntry()

            val oldEntryIdx = oldGroup.entries.indexOfFirst { it.uuid == noteUid }
            if (oldEntryIdx == -1) {
                return@withLock OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_NOTE))
            }

            var newDb = db.getRawDatabase()

            val getAllNotes = all
            if (getAllNotes.isFailed) {
                return@withLock getAllNotes.mapError()
            }

            val attachmentsDiff = differ.getAttachmentsDiff(oldNote, newNote)
            if (attachmentsDiff.isNotEmpty()) {
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
            contentWatcher.notifyEntryChanged(oldNote, newNote)
        }

        return result
    }

    override fun remove(noteUid: UUID): OperationResult<Boolean> {
        val result = db.lock.withLock {
            val getNoteResult = getNoteByUid(noteUid)
            if (getNoteResult.isFailed) {
                return@withLock getNoteResult.mapError()
            }

            val note = getNoteResult.obj
            val newDb = db.getRawDatabase().removeEntry(noteUid)

            db.swapDatabase(newDb)

            db.commit().mapWithObject(note)
        }

        if (result.isSucceededOrDeferred) {
            val note = result.obj
            contentWatcher.notifyEntryRemoved(note)
        }

        return result.mapWithObject(true)
    }

    override fun find(query: String): OperationResult<List<Note>> {
        return db.lock.withLock {
            val allNotesResult = all
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
            contentWatcher.notifyEntryInserted(newNote)
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
}