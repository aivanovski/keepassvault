package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_GROUP_IS_ALREADY_EXIST
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.TemplateDao
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.TEMPLATE_GROUP_NAME
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class TemplateDaoImpl(
    private val groupDao: GroupDao,
    private val noteDao: NoteDao
) : TemplateDao {

    private val templateGroupUidRef = AtomicReference<UUID>()
    private val templatesRef = AtomicReference<List<Template>>(emptyList())

    init {
        noteDao.contentWatcher.subscribe(object : ContentWatcher.OnEntryChangeListener<Note> {
            override fun onEntryChanged(oldEntry: Note, newEntry: Note) {
                checkGroupUid(newEntry.groupUid)
            }
        })

        noteDao.contentWatcher.subscribe(object : ContentWatcher.OnEntryCreateListener<Note> {
            override fun onEntryCreated(entry: Note) {
                checkGroupUid(entry.groupUid)
            }
        })

        noteDao.contentWatcher.subscribe(object : ContentWatcher.OnEntryRemoveListener<Note> {
            override fun onEntryRemoved(entry: Note) {
                checkGroupUid(entry.groupUid)
            }
        })

        groupDao.contentWatcher.subscribe(object : ContentWatcher.OnEntryRemoveListener<Group> {
            override fun onEntryRemoved(entry: Group) {
                checkGroupUid(entry.uid)
            }
        })

        findTemplateNotes()
    }

    override fun getTemplateGroupUid(): OperationResult<UUID?> {
        return OperationResult.success(templateGroupUidRef.get())
    }

    override fun getTemplates(): OperationResult<List<Template>> {
        return OperationResult.success(templatesRef.get() ?: emptyList())
    }

    override fun addTemplates(templates: List<Template>): OperationResult<Boolean> {
        return addTemplates(templates, true)
    }

    override fun addTemplates(
        templates: List<Template>,
        doInterstitialCommits: Boolean
    ): OperationResult<Boolean> {
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

        val templateGroup = GroupEntity(
            title = TEMPLATE_GROUP_NAME,
            parentUid = rootGroup.uid,
            autotypeEnabled = InheritableBooleanOption.DISABLED,
            searchEnabled = InheritableBooleanOption.DISABLED
        )
        val insertGroupResult = groupDao.insert(templateGroup, doInterstitialCommits)
        if (insertGroupResult.isFailed) {
            return insertGroupResult.takeError()
        }

        val templateGroupUid = insertGroupResult.obj
        val notes = templates.map { TemplateNoteFactory.createTemplateNote(it, templateGroupUid) }
        val insertNotesResult = noteDao.insert(notes, doInterstitialCommits)
        if (insertNotesResult.isFailed) {
            return insertNotesResult.takeError()
        }

        return OperationResult.success(true)
    }

    private fun findTemplateNotes(): OperationResult<Boolean> {
        val groupsResult = groupDao.all
        if (groupsResult.isFailed) {
            return groupsResult.takeError()
        }

        val groups = groupsResult.obj
        val templateGroup = groups.firstOrNull { group -> group.title == TEMPLATE_GROUP_NAME }
        // TODO: At some cases 'templateGroup' can be located inside 'Recycle Bin'
        //  in this case it should be ignored
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