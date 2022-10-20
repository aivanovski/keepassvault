package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_NAME_TEMPLATE
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_VALUE_TEMPLATE
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_TITLE
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_TYPE
import java.util.Date
import java.util.UUID

object TemplateNoteFactory {

    fun createTemplateNote(template: Template, templateGroupUid: UUID): Note {
        val properties = mutableListOf<Property>(
            Property(
                name = PROPERTY_NAME_TEMPLATE,
                value = PROPERTY_VALUE_TEMPLATE
            ),
            Property(
                name = PropertyType.TITLE.propertyName,
                type = PropertyType.TITLE,
                value = template.title
            )
        )

        template.fields
            .forEach { field ->
                properties.apply {
                    add(
                        Property(
                            name = TemplateConst.PROPERTY_PREFIX_POSITION + field.title,
                            value = field.position?.toString()
                        )
                    )
                    add(
                        Property(
                            name = PROPERTY_PREFIX_TITLE + field.title,
                            value = field.title
                        )
                    )
                    add(
                        Property(
                            name = PROPERTY_PREFIX_TYPE + field.title,
                            value = field.type?.textName
                        )
                    )
                }
            }

        return Note(
            uid = null,
            groupUid = templateGroupUid,
            created = Date(),
            modified = Date(),
            title = template.title,
            properties = properties
        )
    }
}