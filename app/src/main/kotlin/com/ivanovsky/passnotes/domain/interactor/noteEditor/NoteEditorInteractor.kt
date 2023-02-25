package com.ivanovsky.passnotes.domain.interactor.noteEditor

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError.newErrorMessage
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteUseCase
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.ShaUtils
import java.util.UUID
import kotlinx.coroutines.withContext

class NoteEditorInteractor(
    private val fileSystemResolver: FileSystemResolver,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getDbUseCase: GetDatabaseUseCase,
    private val dispatchers: DispatcherProvider,
    private val observerBus: ObserverBus,
    private val resourceProvider: ResourceProvider
) {

    fun createNewNote(note: Note): OperationResult<Unit> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.mapError()
        }

        val db = getDbResult.obj
        val insertResult = db.noteDao.insert(note)
        if (insertResult.isFailed) {
            return insertResult.mapError()
        }

        observerBus.notifyNoteDataSetChanged(note.groupUid)

        return insertResult.takeStatusWith(Unit)
    }

    fun loadNote(uid: UUID): OperationResult<Note> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.mapError()
        }

        val db = getDbResult.obj
        return db.noteDao.getNoteByUid(uid)
    }

    suspend fun updateNote(note: Note): OperationResult<Unit> =
        updateNoteUseCase.updateNote(note)

    suspend fun loadTemplate(templateUid: UUID): OperationResult<Template?> =
        withContext(dispatchers.IO) {
            val getDbResult = getDbUseCase.getDatabase()
            if (getDbResult.isFailed) {
                return@withContext getDbResult.mapError()
            }

            val db = getDbResult.obj
            val getTemplatesResult = db.templateDao.getTemplates()
            if (getTemplatesResult.isFailed) {
                return@withContext getTemplatesResult.mapError()
            }

            val templates = getTemplatesResult.obj
            val template = templates.firstOrNull { template -> template.uid == templateUid }

            OperationResult.success(template)
        }

    suspend fun createAttachment(
        file: FileDescriptor,
        currentAttachments: Collection<Attachment>
    ): OperationResult<Attachment> =
        withContext(dispatchers.IO) {
            val fsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

            val openFileResult =
                fsProvider.openFileForRead(file, OnConflictStrategy.CANCEL, FSOptions.READ_ONLY)
            if (openFileResult.isFailed) {
                return@withContext openFileResult.mapError()
            }

            val readBytesResult = InputOutputUtils.readAllBytes(
                openFileResult.obj,
                isCloseOnFinish = true
            )
            if (readBytesResult.isFailed) {
                return@withContext readBytesResult.mapError()
            }

            val content = readBytesResult.obj
            val hash = ShaUtils.sha256(content)
            val findExistingResult = findExistingAttachment(hash)
            if (findExistingResult.isFailed) {
                return@withContext findExistingResult.mapError()
            }

            for (current in currentAttachments) {
                if (current.hash == hash) {
                    return@withContext OperationResult.error(
                        newErrorMessage(
                            resourceProvider.getString(
                                R.string.file_is_already_added_with_value,
                                file.name
                            )
                        )
                    )
                }
            }

            val existing = findExistingResult.obj
            if (existing != null) {
                return@withContext OperationResult.success(existing)
            }

            OperationResult.success(
                Attachment(
                    uid = hash.toString(),
                    name = file.name,
                    hash = hash,
                    data = content
                )
            )
        }

    private fun findExistingAttachment(hash: Hash): OperationResult<Attachment?> {
        val getDbResult = getDbUseCase.getDatabaseSynchronously()
        if (getDbResult.isFailed) {
            return getDbResult.mapError()
        }

        val db = getDbResult.obj
        val getAllNotesResult = db.noteDao.all
        if (getAllNotesResult.isFailed) {
            return getAllNotesResult.mapError()
        }

        val allNotes = getAllNotesResult.obj
        for (note in allNotes) {
            for (attachment in note.attachments) {
                if (attachment.hash == hash) {
                    return OperationResult.success(attachment)
                }
            }
        }

        return OperationResult.success(null)
    }
}