package com.ivanovsky.passnotes.data.repository.encdb.dao

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import java.util.UUID

interface NoteDao {
    fun getAll(): OperationResult<List<Note>>
    fun getNotesByGroupUid(groupUid: UUID): OperationResult<List<Note>>
    fun getNoteByUid(noteUid: UUID): OperationResult<Note>
    fun insert(note: Note): OperationResult<UUID>
    fun insert(notes: List<Note>): OperationResult<Boolean>
    fun insert(notes: List<Note>, doCommit: Boolean): OperationResult<Boolean>
    fun update(note: Note): OperationResult<UUID>
    fun remove(noteUid: UUID): OperationResult<Boolean>
    fun find(query: String): OperationResult<List<Note>>
    fun getContentWatcher(): ContentWatcher<Note>
    fun getHistory(uid: UUID): OperationResult<List<Note>>
}