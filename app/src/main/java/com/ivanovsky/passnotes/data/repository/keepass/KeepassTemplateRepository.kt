@file:Suppress("FoldInitializerAndIfToElvis")

package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.TemplateRepository
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class KeepassTemplateRepository(
    private val groupDao: KeepassGroupDao,
    private val noteDao: KeepassNoteDao
) : TemplateRepository {

    private val templateGroupUidRef = AtomicReference<UUID?>()
    private val templatesRef = AtomicReference<List<Template>>(emptyList())

    init {
        noteDao.setOnNoteChangeListener { groupUid, oldNoteUid, newNoteUid ->
            onNoteUpdated(groupUid, oldNoteUid, newNoteUid)
        }
        noteDao.setOnNoteInsertListener { groupUid, noteUid ->
            onNoteInserted(groupUid, noteUid)
        }
    }

    override fun getTemplates(): List<Template>? {
        return templatesRef.get()
    }

    fun findTemplateNotes(): OperationResult<Boolean> {
        val groupsResult = groupDao.all
        if (groupsResult.isFailed) {
            return groupsResult.takeError()
        }

        val groups = groupsResult.obj
        val templateGroup = groups.firstOrNull { group -> group.title == TEMPLATE_GROUP_NAME }
        if (templateGroup == null) {
            templatesRef.set(null)
            return OperationResult.success(false)
        }

        val notesResult = noteDao.getNotesByGroupUid(templateGroup.uid)
        if (notesResult.isFailed) {
            return notesResult.takeError()
        }

        val templateNotes = notesResult.obj
        val templates = templateNotes.mapNotNull { note -> TemplateParser.parse(note) }
            .sortedBy { template -> template.title }

        templatesRef.set(templates)

        return OperationResult.success(true)
    }

    private fun onNoteUpdated(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID) {
        if (templateGroupUidRef.get() != groupUid) return

        val templates = templatesRef.get() ?: return
        val updatedTemplate = templates.firstOrNull { template -> template.uid == oldNoteUid }
        if (updatedTemplate == null) {
            return
        }

        // TODO: refactor, we can just replace updated note in templates list

        val findResult = findTemplateNotes()
        if (findResult.isFailed) {
            templatesRef.set(null)
            return
        }
    }

    private fun onNoteInserted(groupUid: UUID, noteUid: UUID) {
        if (templateGroupUidRef.get() != groupUid) return

        // TODO: refactor

        val findResult = findTemplateNotes()
        if (findResult.isFailed) {
            templatesRef.set(null)
            return
        }
    }

    companion object {
        private const val TEMPLATE_GROUP_NAME = "Templates"
    }

}