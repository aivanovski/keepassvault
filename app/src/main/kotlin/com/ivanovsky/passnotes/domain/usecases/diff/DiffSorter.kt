package com.ivanovsky.passnotes.domain.usecases.diff

import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.PropertyType.Companion.DEFAULT_TYPES
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEventType
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlin.reflect.KClass

class DiffSorter {

    fun sort(
        events: List<DiffEvent<EncryptedDatabaseElement>>
    ): List<DiffEvent<EncryptedDatabaseElement>> {
        return events.splitEventByType().values
            .map { eventsByType ->
                eventsByType.splitByEntityType()
                    .map { (type, eventsByEntityType) ->
                        if (type == Property::class) {
                            val (defaultFields, otherFields) = eventsByEntityType
                                .asPropertyEvents()
                                .splitDefaultAndOtherFields()

                            defaultFields.sortDefaultFields() + otherFields.sortByName()
                        } else {
                            eventsByEntityType.sortByName()
                        }
                    }
            }
            .flatten()
            .flatten()
            .asDatabaseElementEvents()
    }

    private fun List<DiffEvent<EncryptedDatabaseElement>>.splitEventByType():
        Map<DiffEventType, List<DiffEvent<EncryptedDatabaseElement>>> {
        val updateEvents = this.mapNotNull { event ->
            if (event is DiffEvent.Update<*>) event else null
        }

        val deleteEvents = this.mapNotNull { event ->
            if (event is DiffEvent.Delete<*>) event else null
        }

        val insertEvents = this.mapNotNull { event ->
            if (event is DiffEvent.Insert<*>) event else null
        }

        return mapOf(
            DiffEventType.UPDATE to updateEvents,
            DiffEventType.DELETE to deleteEvents,
            DiffEventType.INSERT to insertEvents
        )
    }

    private fun List<DiffEvent<EncryptedDatabaseElement>>.splitByEntityType():
        Map<KClass<out EncryptedDatabaseElement>, List<DiffEvent<EncryptedDatabaseElement>>> {
        val groupEvents = this.mapNotNull { event ->
            if (event.getEntity() is Group) event else null
        }

        val entryEvents = this.mapNotNull { event ->
            if (event.getEntity() is Note) event else null
        }

        val fieldEvents = this.mapNotNull { event ->
            if (event.getEntity() is Property) event else null
        }

        return mapOf(
            Group::class to groupEvents,
            Note::class to entryEvents,
            Property::class to fieldEvents
        )
    }

    private fun List<DiffEvent<Property>>.splitDefaultAndOtherFields():
        Pair<List<DiffEvent<Property>>, List<DiffEvent<Property>>> {
        return this.partition { event ->
            val type = event.getEntity().type
            type in DEFAULT_TYPES
        }
    }

    private fun <T : EncryptedDatabaseElement> List<DiffEvent<T>>.sortByName():
        List<DiffEvent<T>> {
        return this.sortedBy { event -> event.getEntity().getName() }
    }

    private fun List<DiffEvent<Property>>.sortDefaultFields():
        List<DiffEvent<Property>> {
        return this.sortedBy { event ->
            val type = event.getEntity().type
            DEFAULT_PROPERTY_ORDER[type] ?: Int.MAX_VALUE
        }
    }

    private fun EncryptedDatabaseElement.getName(): String {
        return when (this) {
            is Group -> title
            is Note -> title
            is Property -> name ?: EMPTY
            else -> throw IllegalStateException()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun List<DiffEvent<EncryptedDatabaseElement>>.asPropertyEvents():
        List<DiffEvent<Property>> {
        return this as List<DiffEvent<Property>>
    }

    @Suppress("UNCHECKED_CAST")
    private fun List<DiffEvent<out EncryptedDatabaseElement>>.asDatabaseElementEvents():
        List<DiffEvent<EncryptedDatabaseElement>> {
        return this as List<DiffEvent<EncryptedDatabaseElement>>
    }

    companion object {
        private val DEFAULT_PROPERTY_ORDER = mapOf(
            PropertyType.TITLE to 1,
            PropertyType.USER_NAME to 2,
            PropertyType.PASSWORD to 3,
            PropertyType.OTP to 4,
            PropertyType.URL to 5,
            PropertyType.NOTES to 6
        )
    }
}