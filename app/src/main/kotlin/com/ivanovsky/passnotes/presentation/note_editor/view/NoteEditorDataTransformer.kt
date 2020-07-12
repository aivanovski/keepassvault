package com.ivanovsky.passnotes.presentation.note_editor.view

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_CUSTOM
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_NOTES
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem.Companion.ITEM_ID_PASSWORD
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

class NoteEditorDataTransformer {

    @Inject
    lateinit var resources: ResourceHelper

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    fun createNoteToEditorItems(note: Note): List<BaseDataItem> {
        val items = mutableListOf<BaseDataItem>()

        val propSpreader = PropertySpreader(note.properties)
        val userName = propSpreader.getVisiblePropertyValueByType(PropertyType.USER_NAME)
        val url = propSpreader.getVisiblePropertyValueByType(PropertyType.URL)
        val notes = propSpreader.getVisiblePropertyValueByType(PropertyType.NOTES)
        val password = propSpreader.getVisiblePropertyValueByType(PropertyType.PASSWORD)
        val otherProperties = propSpreader.getCustomProperties()

        items.add(
            TextDataItem(
                ITEM_ID_TITLE,
                resources.getString(R.string.title),
                note.title,
                TextInputType.TEXT_CAP_SENTENCES,
                SINGLE_LINE,
                isShouldNotBeEmpty = true
            )
        )

        items.add(
            TextDataItem(
                ITEM_ID_USER_NAME,
                resources.getString(R.string.username),
                userName ?: "",
                TextInputType.TEXT,
                SINGLE_LINE
            )
        )

        items.add(
            SecretDataItem(
                ITEM_ID_PASSWORD,
                resources.getString(R.string.password),
                password ?: "",
                SecretInputType.TEXT
            )
        )

        items.add(
            TextDataItem(
                ITEM_ID_URL,
                resources.getString(R.string.url_cap),
                url ?: "",
                TextInputType.URL,
                MULTIPLE_LINES
            )
        )

        items.add(
            TextDataItem(
                ITEM_ID_NOTES,
                resources.getString(R.string.notes),
                notes ?: "",
                TextInputType.TEXT_CAP_SENTENCES,
                MULTIPLE_LINES
            )
        )

        for (property in otherProperties) {
            items.add(ExtTextDataItem(
                ITEM_ID_CUSTOM,
                property.name ?: "",
                property.value ?: "",
                isProtected = property.isProtected,
                isCollapsed = true,
                textInputType = TextInputType.TEXT
            ))
        }

        return items
    }

    fun createPropertiesFromItems(items: List<BaseDataItem>): List<Property> {
        val properties = mutableListOf<Property>()

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

        if (!userName.isNullOrEmpty()) {
            val userNameProperty = Property(
                PropertyType.USER_NAME,
                PropertyType.USER_NAME.propertyName,
                userName
            )

            properties.add(userNameProperty)
        }

        if (!url.isNullOrEmpty()) {
            val property = Property(PropertyType.URL, PropertyType.URL.propertyName, url)
            properties.add(property)
        }

        if (!notes.isNullOrEmpty()) {
            val property = Property(PropertyType.NOTES, PropertyType.NOTES.propertyName, notes)
            properties.add(property)
        }

        if (!password.isNullOrEmpty()) {
            val property = Property(
                PropertyType.PASSWORD,
                PropertyType.PASSWORD.propertyName,
                password,
                isProtected = true
            )
            properties.add(property)
        }

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
}