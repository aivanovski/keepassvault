@file:Suppress("FoldInitializerAndIfToElvis")

package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_GROUP_IS_ALREADY_EXIST
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.TemplateRepository
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.TEMPLATE_GROUP_NAME
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class KeepassTemplateRepository(
    private val groupDao: KeepassGroupDao,
    private val noteDao: KeepassNoteDao
) : TemplateRepository {

    private val templateGroupUidRef = AtomicReference<UUID>()
    private val templatesRef = AtomicReference<List<Template>>(emptyList())

    init {
        noteDao.addOnNoteChangeListener { groupUid, _, _ ->
            checkGroupUid(groupUid)
        }
        noteDao.addOnNoteInsertListener { groupAndNoteUids ->
            val groupUids = groupAndNoteUids
                .map { it.first }
                .toSet()

            groupUids.forEach { groupUid ->
                checkGroupUid(groupUid)
            }
        }
        noteDao.addOnNoteRemoveListener { groupUid, _ ->
            checkGroupUid(groupUid)
        }
        groupDao.addOnGroupRemoveLister { groupUid ->
            checkGroupUid(groupUid)
        }
    }

    override fun getTemplateGroupUid(): OperationResult<UUID?> {
        return OperationResult.success(templateGroupUidRef.get())
    }

    override fun getTemplates(): OperationResult<List<Template>> {
        return OperationResult.success(templatesRef.get() ?: emptyList())
    }

    override fun addTemplates(templates: List<Template>): OperationResult<Boolean> {
        val rootGroupResult = groupDao.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val rootGroup = rootGroupResult.obj
        val groupsResult = groupDao.getChildGroups(rootGroup.uid)
        if (groupsResult.isFailed) {
            return groupsResult.takeError()
        }

        val groups = groupsResult.obj
        if (groups.any { it.title == TEMPLATE_GROUP_NAME }) {
            return OperationResult.error(
                newDbError(
                    String.format(GENERIC_MESSAGE_GROUP_IS_ALREADY_EXIST, TEMPLATE_GROUP_NAME)
                )
            )
        }

        val templateGroup = Group(
            title = TEMPLATE_GROUP_NAME
        )
        val insertGroupResult = groupDao.insert(templateGroup, rootGroup.uid)
        if (insertGroupResult.isFailed) {
            return insertGroupResult.takeError()
        }

        val templateGroupUid = insertGroupResult.obj
        val notes = templates.map { TemplateNoteFactory.createTemplateNote(it, templateGroupUid) }
        val insertNotesResult = noteDao.insert(notes)
        if (insertNotesResult.isFailed) {
            return insertNotesResult.takeError()
        }

        return OperationResult.success(true)
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
            templateGroupUidRef.set(null)
            return OperationResult.success(false)
        }

        val notesResult = noteDao.getNotesByGroupUid(templateGroup.uid)
        if (notesResult.isFailed) {
            return notesResult.takeError()
        }

        val templateNotes = notesResult.obj
        val templates = templateNotes
            .mapNotNull { note -> TemplateParser.parse(note) }
            .sortedBy { template -> template.title }

        templatesRef.set(templates)

        return OperationResult.success(true)
    }

    private fun checkGroupUid(groupUid: UUID) {
        val templateGroupUid = templateGroupUidRef.get()
        if (templateGroupUid != null && templateGroupUid != groupUid) return

        // TODO: refactor

        val findResult = findTemplateNotes()
        if (findResult.isFailed) {
            templatesRef.set(null)
            return
        }
    }
}