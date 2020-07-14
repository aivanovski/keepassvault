@file:Suppress("IfThenToElvis")

package com.ivanovsky.passnotes.presentation.note_editor.view

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.*
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_CUSTOM
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_NOTES
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_PASSWORD
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_TEMPLATE
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_TITLE
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_URL
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_USER_NAME
import com.ivanovsky.passnotes.presentation.note_editor.view.extended_text.ExtTextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.text.InputLines.MULTIPLE_LINES
import com.ivanovsky.passnotes.presentation.note_editor.view.text.InputLines.SINGLE_LINE
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType
import javax.inject.Inject

class NoteEditorDataTransformer(
    private val template: Template?
) {

    @Inject
    lateinit var resources: ResourceHelper

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    fun createEditorItemsForNewNote(): List<BaseDataItem> {
        return if (template != null) {
            createEditorItemsFromTemplate(template)
        } else {
            createDefaultEditorItems()
        }
    }

    private fun createEditorItemsFromTemplate(template: Template): List<BaseDataItem> {
        val items = mutableListOf<BaseDataItem>()

        items.add(createTitleItem(""))

        for (field in template.fields) {
            val isProtected = (field.type == TemplateFieldType.PROTECTED_INLINE)

            items.add(
                ExtTextDataItem(
                    ITEM_ID_TEMPLATE,
                    field.title,
                    "",
                    isProtected = isProtected,
                    isCollapsed = true,
                    textInputType = TextInputType.TEXT
                )
            )
        }

        return items
    }

    private fun createDefaultEditorItems(): List<BaseDataItem> {
        return listOf(
            createTitleItem(""),
            createUserNameItem(""),
            createPasswordItem(""),
            createUrlItem(""),
            createNotesItem("")
        )
    }

    fun createNoteToEditorItems(note: Note): List<BaseDataItem> {
        val items = mutableListOf<BaseDataItem>()

        val propSpreader = PropertySpreader(note.properties)

        val template = this.template

        if (template != null) {
            items.add(createTitleItem(note.title))
            
            for (field in template.fields) {
                val property = propSpreader.getPropertyByName(field.title)

                val isProtected = if (property != null) {
                    property.isProtected
                } else {
                    field.type == TemplateFieldType.PROTECTED_INLINE
                }
                    
                items.add(
                    ExtTextDataItem(
                        ITEM_ID_TEMPLATE,
                        field.title ?: "",
                        property?.value ?: "",
                        isProtected = isProtected,
                        isCollapsed = true,
                        textInputType = TextInputType.TEXT
                    )
                )
            }

            val otherProperties = propSpreader.excludeTemplateRelatedProperties(template)

            for (property in otherProperties) {
                items.add(
                    ExtTextDataItem(
                        ITEM_ID_CUSTOM,
                        property.name ?: "",
                        property.value ?: "",
                        isProtected = property.isProtected,
                        isCollapsed = true,
                        textInputType = TextInputType.TEXT
                    )
                )
            }

        } else {
            val userName = propSpreader.getVisiblePropertyValueByType(PropertyType.USER_NAME)
            val url = propSpreader.getVisiblePropertyValueByType(PropertyType.URL)
            val notes = propSpreader.getVisiblePropertyValueByType(PropertyType.NOTES)
            val password = propSpreader.getVisiblePropertyValueByType(PropertyType.PASSWORD)
            val otherProperties = propSpreader.getCustomProperties()

            items.add(createTitleItem(note.title))
            items.add(createUserNameItem(userName ?: ""))
            items.add(createPasswordItem(password ?: ""))
            items.add(createUrlItem(url ?: ""))
            items.add(createNotesItem(notes ?: ""))

            for (property in otherProperties) {
                items.add(
                    ExtTextDataItem(
                        ITEM_ID_CUSTOM,
                        property.name ?: "",
                        property.value ?: "",
                        isProtected = property.isProtected,
                        isCollapsed = true,
                        textInputType = TextInputType.TEXT
                    )
                )
            }
        }

        return items
    }

    fun createPropertiesFromItems(items: List<BaseDataItem>): List<Property> {
        val properties = mutableListOf<Property>()

        val title = getValueByItemId(ITEM_ID_TITLE, items)
        val userName = getValueByItemId(ITEM_ID_USER_NAME, items)
        val url = getValueByItemId(ITEM_ID_URL, items)
        val notes = getValueByItemId(ITEM_ID_NOTES, items)
        val password = getValueByItemId(ITEM_ID_PASSWORD, items)

        val excludedItemIds = setOf(
            ITEM_ID_TITLE,
            ITEM_ID_USER_NAME,
            ITEM_ID_PASSWORD,
            ITEM_ID_NOTES,
            ITEM_ID_URL
        )
        val otherItems = excludeItemsById(items, excludedItemIds)

        properties.add(
            Property(
                PropertyType.TITLE,
                PropertyType.TITLE.propertyName,
                title ?: ""
            )
        )
        properties.add(
            Property(
                PropertyType.USER_NAME,
                PropertyType.USER_NAME.propertyName,
                userName ?: ""
            )
        )
        properties.add(
            Property(
                PropertyType.URL,
                PropertyType.URL.propertyName,
                url ?: ""
            )
        )
        properties.add(
            Property(
                PropertyType.NOTES,
                PropertyType.NOTES.propertyName,
                notes ?: ""
            )
        )

        properties.add(
            Property(
                PropertyType.PASSWORD,
                PropertyType.PASSWORD.propertyName,
                password ?: "",
                isProtected = true
            )
        )

        if (otherItems.isNotEmpty()) {
            for (item in otherItems) {
                if (item is ExtTextDataItem) {
                    val property = Property(
                        null,
                        item.name,
                        item.value,
                        isProtected = item.isProtected
                    )
                    properties.add(property)
                }
            }
        }

        return properties
    }

    fun getTitleFromItems(items: List<BaseDataItem>): String? {
        return getValueByItemId(ITEM_ID_TITLE, items)
    }

    private fun getValueByItemId(id: Int, items: List<BaseDataItem>): String? {
        return items.find { item -> item.id == id }
            ?.value
    }

    fun filterNotEmptyItems(items: List<BaseDataItem>): List<BaseDataItem> {
        return items.filter { item -> !item.isEmpty }
    }

    private fun excludeItemsById(
        items: List<BaseDataItem>,
        excludeIds: Set<Int>
    ): List<BaseDataItem> {
        return items.filter { item -> !excludeIds.contains(item.id) }
    }

    private fun createTitleItem(title: String): BaseDataItem {
        return TextDataItem(
            ITEM_ID_TITLE,
            resources.getString(R.string.title),
            title,
            TextInputType.TEXT_CAP_SENTENCES,
            SINGLE_LINE,
            isShouldNotBeEmpty = true
        )
    }

    private fun createUserNameItem(userName: String): BaseDataItem {
        return TextDataItem(
            ITEM_ID_USER_NAME,
            resources.getString(R.string.username),
            userName,
            TextInputType.TEXT,
            SINGLE_LINE
        )
    }

    private fun createPasswordItem(password: String): BaseDataItem {
        return SecretDataItem(
            ITEM_ID_PASSWORD,
            resources.getString(R.string.password),
            password,
            SecretInputType.TEXT
        )
    }

    private fun createUrlItem(url: String): BaseDataItem {
        return TextDataItem(
            ITEM_ID_URL,
            resources.getString(R.string.url_cap),
            url,
            TextInputType.URL,
            MULTIPLE_LINES
        )
    }

    private fun createNotesItem(notes: String): BaseDataItem {
        return TextDataItem(
            ITEM_ID_NOTES,
            resources.getString(R.string.notes),
            notes,
            TextInputType.TEXT_CAP_SENTENCES,
            MULTIPLE_LINES
        )
    }
}