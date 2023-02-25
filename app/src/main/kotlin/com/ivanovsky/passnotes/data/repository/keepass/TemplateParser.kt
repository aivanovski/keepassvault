package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_NAME_TEMPLATE
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.entity.TemplateField
import com.ivanovsky.passnotes.data.entity.TemplateFieldType
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_POSITION
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_TITLE
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_TYPE
import com.ivanovsky.passnotes.domain.entity.PropertySpreader
import com.ivanovsky.passnotes.util.isDigitsOnly

object TemplateParser {

    fun parse(note: Note): Template? {
        val uid = note.uid ?: return null

        val properties = PropertySpreader(note.properties).getTemplateProperties()

        val propertySpreader = PropertySpreader(properties)

        val templateIndicator = propertySpreader.getPropertyByName(PROPERTY_NAME_TEMPLATE)
        if (templateIndicator == null || templateIndicator.value != "1") {
            return null
        }

        val titles = propertySpreader.getPropertiesWithPrefix(PROPERTY_PREFIX_TITLE)
        val templateFieldMap = mutableMapOf<String, MutableTemplate>()

        for (property in titles) {
            if (property.name == null || property.value == null) {
                continue
            }

            val keyStartIdx = PROPERTY_PREFIX_TITLE.length
            if (keyStartIdx >= property.name.length) {
                continue
            }

            val key = property.name.substring(keyStartIdx)
            val title = property.value

            templateFieldMap[key] = MutableTemplate(title)
        }

        for (property in properties) {
            if (property.name == null || property.value == null) {
                continue
            }

            val key = getPropertyKey(
                property.name
            ) ?: continue

            val templateField = templateFieldMap[key] ?: continue

            if (property.name.startsWith(PROPERTY_PREFIX_TYPE)) {
                templateField.type = TemplateFieldType.fromTextName(property.value)
            } else if (property.name.startsWith(PROPERTY_PREFIX_POSITION) &&
                property.value.isDigitsOnly()
            ) {
                templateField.position = property.value.toInt()
            }
        }

        val fields = templateFieldMap.values
            .filter { field -> field.position != null || field.type != null }
            .sortedBy { field -> field.position ?: Integer.MAX_VALUE }
            .map { field -> TemplateField(field.title, field.position, field.type) }
        if (fields.isEmpty()) {
            return null
        }

        return Template(uid, note.title, fields)
    }

    private fun getPropertyKey(propertyName: String): String? {
        val prefixEndIdx = getPrefixEnd(
            propertyName
        ) ?: return null

        if (prefixEndIdx >= propertyName.length - 1) return null

        return propertyName.substring(prefixEndIdx + 1)
    }

    private fun getPrefixEnd(name: String): Int? {
        var underscoreCount = 0
        var i = 0

        while (i < name.length) {
            val ch = name[i]

            if (ch == '_') {
                underscoreCount++

                if (underscoreCount == 3) {
                    return i
                }
            }

            i++
        }

        return null
    }

    data class MutableTemplate(
        val title: String,
        var position: Int? = null,
        var type: TemplateFieldType? = null
    )
}