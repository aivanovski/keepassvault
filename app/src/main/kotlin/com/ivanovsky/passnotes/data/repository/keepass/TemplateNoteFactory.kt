package com.ivanovsky.passnotes.data.repository.keepass

import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_NAME_TEMPLATE
import com.ivanovsky.passnotes.data.entity.Property.Companion.PROPERTY_VALUE_TEMPLATE
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_TITLE
import com.ivanovsky.passnotes.data.repository.keepass.TemplateConst.PROPERTY_PREFIX_TYPE
import java.util.Date
import java.util.UUID

object TemplateNoteFactory {

    fun createTemplateNote(template: Template, templateGroupUid: UUID): Note {
        val properties = template
            .fields
            .flatMap { field ->
                listOf(
                    Property(
                        name = TemplateConst.PROPERTY_PREFIX_POSITION + field.title,
                        value = field.position?.toString()
                    ),
                    Property(
                        name = PROPERTY_PREFIX_TITLE + field.title,
                        value = field.title
                    ),
                    Property(
                        name = PROPERTY_PREFIX_TYPE + field.title,
                        value = field.type?.name
                    ),
                )
            }
            .toMutableList()

        properties.add(
            index = 0,
            element = Property(
                name = PROPERTY_NAME_TEMPLATE,
                value = PROPERTY_VALUE_TEMPLATE
            )
        )

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