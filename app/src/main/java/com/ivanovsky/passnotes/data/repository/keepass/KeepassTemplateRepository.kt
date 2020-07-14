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
        noteDao.setOnNoteChangeListener { groupUid, _, _ ->
            checkGroupUid(groupUid)
        }
        noteDao.setOnNoteInsertListener { groupUid, _ ->
            checkGroupUid(groupUid)
        }
        noteDao.setOnNoteRemoveListener { groupUid, _ ->
            checkGroupUid(groupUid)
        }
        groupDao.setOnGroupRemoveLister { groupUid ->
            checkGroupUid(groupUid)
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

    private fun checkGroupUid(groupUid: UUID) {
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